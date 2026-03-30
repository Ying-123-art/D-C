package com.example.giuaky

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(postId: String, onBackToHome: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var existingImageUrl by remember { mutableStateOf("") }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var isLoadingPost by remember { mutableStateOf(true) }

    val database = FirebaseDatabase.getInstance().getReference("posts").child(postId)

    LaunchedEffect(postId) {
        database.get().addOnSuccessListener { snapshot ->
            val post = snapshot.getValue(Post::class.java)
            if (post != null) {
                title = post.title
                content = post.content
                existingImageUrl = post.imageUrl
            }
            isLoadingPost = false
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        newImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Post") },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoadingPost) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    enabled = !isUploading
                )

                Spacer(modifier = Modifier.height(16.dp))

                val displayImage = newImageUri ?: if (existingImageUrl.isNotEmpty()) Uri.parse(existingImageUrl) else null
                if (displayImage != null) {
                    Image(
                        painter = rememberAsyncImagePainter(displayImage),
                        contentDescription = "Post Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                OutlinedButton(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    Text("Change Image")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isUploading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            isUploading = true
                            if (newImageUri != null) {
                                uploadNewImageAndUpdate(postId, title, content, newImageUri!!, onBackToHome)
                            } else {
                                updatePost(postId, title, content, existingImageUrl, onBackToHome)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = title.isNotEmpty() && content.isNotEmpty()
                    ) {
                        Text("Update Post")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            isUploading = true
                            deletePost(postId, existingImageUrl, onBackToHome)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        enabled = !isUploading
                    ) {
                        Text("Delete Post")
                    }
                }
            }
        }
    }
}

fun uploadNewImageAndUpdate(postId: String, title: String, content: String, uri: Uri, onSuccess: () -> Unit) {
    val storageRef = FirebaseStorage.getInstance().reference
    val fileName = "images/${UUID.randomUUID()}.jpg"
    val imageRef = storageRef.child(fileName)

    imageRef.putFile(uri)
        .addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                updatePost(postId, title, content, downloadUrl.toString(), onSuccess)
            }
        }
}

fun updatePost(postId: String, title: String, content: String, imageUrl: String, onSuccess: () -> Unit) {
    val database = FirebaseDatabase.getInstance().getReference("posts").child(postId)
    val updates = mapOf(
        "title" to title,
        "content" to content,
        "imageUrl" to imageUrl
    )
    database.updateChildren(updates).addOnSuccessListener {
        onSuccess()
    }
}

fun deletePost(postId: String, imageUrl: String, onSuccess: () -> Unit) {
    val database = FirebaseDatabase.getInstance().getReference("posts").child(postId)
    database.removeValue().addOnSuccessListener {
        if (imageUrl.isNotEmpty()) {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageRef.delete().addOnCompleteListener {
                onSuccess()
            }
        } else {
            onSuccess()
        }
    }
}
