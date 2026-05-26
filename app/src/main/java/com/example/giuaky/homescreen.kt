package com.example.giuaky

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

data class Post(
    val id: String = "",
    val userId: String = "",
    val authorName: String = "Anonymous",
    val title: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val location: String = "", // Thêm trường vị trí
    val timestamp: Long = 0
)

@Composable
fun HomeScreen(onNavigateToCreate: () -> Unit, onNavigateToEdit: (String) -> Unit) {
    var posts by remember { mutableStateOf(listOf<Post>()) }
    var searchQuery by remember { mutableStateOf("") }
    val database = FirebaseDatabase.getInstance().getReference("posts")
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val postList = mutableListOf<Post>()
                for (data in snapshot.children) {
                    val post = data.getValue(Post::class.java)?.copy(id = data.key ?: "")
                    if (post != null) postList.add(post)
                }
                posts = postList.sortedByDescending { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    val filteredPosts = posts.filter { 
        it.title.contains(searchQuery, ignoreCase = true) || 
        it.content.contains(searchQuery, ignoreCase = true) ||
        it.location.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToCreate() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Đăng bài")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm kiếm bài đăng, vị trí...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredPosts) { post ->
                    PostItem(
                        post = post,
                        isOwner = post.userId == currentUserId,
                        onEditClick = { onNavigateToEdit(post.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PostItem(post: Post, isOwner: Boolean, onEditClick: () -> Unit) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(post.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            // Header: Author info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter("https://ui-avatars.com/api/?name=${post.authorName}&background=random"),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = post.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        if (post.location.isNotEmpty()) {
                            Text(" • ", color = Color.Gray, fontSize = 12.sp)
                            Icon(
                                Icons.Default.LocationOn, 
                                contentDescription = null, 
                                modifier = Modifier.size(12.dp),
                                tint = Color.Gray
                            )
                            Text(
                                text = post.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                if (isOwner) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                    }
                }
            }

            // Title and Content
            if (post.title.isNotEmpty()) {
                Text(
                    text = post.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Text(
                text = post.content,
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyLarge
            )

            // Image
            if (post.imageUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(post.imageUrl),
                    contentDescription = "Hình ảnh bài đăng",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp),
                    contentScale = ContentScale.FillWidth
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp), thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TextButton(onClick = { }) { Text("Thích", color = Color.Gray) }
                TextButton(onClick = { }) { Text("Bình luận", color = Color.Gray) }
                TextButton(onClick = { }) { Text("Chia sẻ", color = Color.Gray) }
            }
        }
    }
}
