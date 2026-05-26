package com.example.giuaky

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(onBackToHome: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var locationName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getCurrentLocation(context, fusedLocationClient) { location ->
                locationName = location
            }
        } else {
            Toast.makeText(context, "Quyền truy cập vị trí bị từ chối", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo bài đăng") },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
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
                label = { Text("Tiêu đề (không bắt buộc)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploading,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Bạn đang nghĩ gì?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                enabled = !isUploading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hiển thị vị trí nếu có
            if (locationName.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(text = locationName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
                    IconButton(onClick = { locationName = "" }) {
                        Text("X", color = androidx.compose.ui.graphics.Color.Red)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { imageLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    enabled = !isUploading
                ) {
                    Text(if (imageUri == null) "Thêm ảnh" else "Đổi ảnh")
                }

                OutlinedButton(
                    onClick = {
                        if (hasLocationPermission(context)) {
                            getCurrentLocation(context, fusedLocationClient) { location ->
                                locationName = location
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isUploading
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Vị trí")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 8.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isUploading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            isUploading = true
                            val authorName = user.displayName ?: user.email?.substringBefore("@") ?: "Người dùng"
                            if (imageUri != null) {
                                uploadImageAndPost(title, content, user.uid, authorName, locationName, imageUri!!, onBackToHome)
                            } else {
                                savePost(title, content, user.uid, authorName, locationName, "", onBackToHome)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = content.isNotEmpty()
                ) {
                    Text("Đăng")
                }
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
           ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private fun getCurrentLocation(context: Context, client: com.google.android.gms.location.FusedLocationProviderClient, onLocationReceived: (String) -> Unit) {
    try {
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val city = address.locality ?: address.subAdminArea ?: "Vị trí lạ"
                        onLocationReceived(city)
                    } else {
                        onLocationReceived("${location.latitude}, ${location.longitude}")
                    }
                } else {
                    Toast.makeText(context, "Không thể lấy vị trí. Hãy bật GPS", Toast.LENGTH_SHORT).show()
                }
            }
    } catch (e: SecurityException) {
        // Handle error
    }
}

fun uploadImageAndPost(title: String, content: String, userId: String, authorName: String, location: String, uri: Uri, onSuccess: () -> Unit) {
    val storageRef = FirebaseStorage.getInstance().reference
    val fileName = "images/${UUID.randomUUID()}.jpg"
    val imageRef = storageRef.child(fileName)

    imageRef.putFile(uri)
        .addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                savePost(title, content, userId, authorName, location, downloadUrl.toString(), onSuccess)
            }
        }
}

fun savePost(title: String, content: String, userId: String, authorName: String, location: String, imageUrl: String, onSuccess: () -> Unit) {
    val database = FirebaseDatabase.getInstance().getReference("posts")
    val postId = database.push().key ?: return
    val post = mapOf(
        "title" to title,
        "content" to content,
        "userId" to userId,
        "authorName" to authorName,
        "location" to location,
        "imageUrl" to imageUrl,
        "timestamp" to System.currentTimeMillis()
    )
    database.child(postId).setValue(post).addOnSuccessListener {
        onSuccess()
    }
}
