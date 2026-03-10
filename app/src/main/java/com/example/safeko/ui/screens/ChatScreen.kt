package com.example.safeko.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.safeko.MainActivity
import com.example.safeko.data.model.Circle
import com.example.safeko.data.model.Message
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.*

// ── Colour tokens ────────────────────────────────────────────
private val Bg          = Color(0xFF0C0D11)
private val AppBarBg    = Color(0xFF0C0D11)
private val BubbleSent  = Color(0xFF2B5FF3)
private val BubbleRecv  = Color(0xFF1E2030)
private val PillBg      = Color(0xFF1A1B20)
private val SheetBg     = Color(0xFF161820)
private val TxtWhite    = Color(0xFFECEDEF)
private val TxtMuted    = Color(0xFF6B7280)
private val AvatarBg    = Color(0xFF7C3AED)
private val DangerRed   = Color(0xFFEF4444)
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    circleId: String,
    onBack: () -> Unit
) {
    val context         = LocalContext.current
    val activity        = context.findActivity()
    var circle          by remember { mutableStateOf<Circle?>(null) }
    var messages        by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText     by remember { mutableStateOf("") }
    val listState       = rememberLazyListState()

    // Sheet / dialog state
    var showOptions     by remember { mutableStateOf(false) }
    var showMembers     by remember { mutableStateOf(false) }
    var showQrDialog    by remember { mutableStateOf(false) }

    DisposableEffect(activity) {
        (activity as? MainActivity)?.setChatSystemBarsEnabled(true)
        onDispose { (activity as? MainActivity)?.setChatSystemBarsEnabled(false) }
    }

    val currentUser     = Firebase.auth.currentUser
    val currentUserId   = currentUser?.uid ?: ""
    val currentUserName = currentUser?.displayName ?: "User"
    val isAdmin         = circle?.ownerId == currentUserId

    LaunchedEffect(circleId) {
        Firebase.firestore.collection("circles").document(circleId)
            .addSnapshotListener { snap, _ ->
                if (snap != null) circle = snap.toObject(Circle::class.java)
            }
    }

    LaunchedEffect(circleId) {
        Firebase.firestore.collection("circles").document(circleId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) messages = snapshot.toObjects(Message::class.java)
            }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun sendMessage() {
        if (messageText.isBlank()) return
        val msg = Message(
            id               = UUID.randomUUID().toString(),
            senderId         = currentUserId,
            senderName       = currentUserName,
            senderProfileUrl = currentUser?.photoUrl?.toString(),
            text             = messageText,
            timestamp        = System.currentTimeMillis()
        )
        Firebase.firestore.collection("circles").document(circleId)
            .collection("messages").document(msg.id).set(msg)
            .addOnSuccessListener { messageText = "" }
            .addOnFailureListener {
                Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Layout ────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
    ) {
        // ── Top bar ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppBarBg)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TxtWhite)
            }

            if (!circle?.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model              = circle!!.imageUrl,
                    contentDescription = null,
                    modifier           = Modifier.size(38.dp).clip(CircleShape),
                    contentScale       = ContentScale.Crop
                )
            } else {
                Box(
                    modifier         = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AvatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = circle?.name?.take(1)?.uppercase() ?: "G",
                        color      = TxtWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text       = circle?.name ?: "Group Chat",
                color      = TxtWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 17.sp,
                modifier   = Modifier.weight(1f)
            )

            IconButton(onClick = { showOptions = true }) {
                Icon(Icons.Rounded.MoreVert, "More options", tint = TxtWhite)
            }
        }

        HorizontalDivider(color = Color(0xFF1E1F22), thickness = 1.dp)

        // ── Messages ────────────────────────────────────────────
        LazyColumn(
            state               = listState,
            modifier            = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding      = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(msg, isMe = msg.senderId == currentUserId)
            }
        }

        // ── Input bar ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Bg)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier         = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(PillBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    "Camera",
                    tint     = TxtMuted,
                    modifier = Modifier.size(22.dp).clickable { }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier         = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (messageText.isBlank()) {
                        Text("Your message here...", color = TxtMuted, fontSize = 15.sp)
                    }
                    BasicTextField(
                        value         = messageText,
                        onValueChange = { messageText = it },
                        textStyle     = TextStyle(color = TxtWhite, fontSize = 15.sp),
                        cursorBrush   = SolidColor(TxtWhite),
                        maxLines      = 4,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (messageText.isNotBlank()) {
                Box(
                    modifier         = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4A90D9))
                        .clickable { sendMessage() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Send, "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mic,            "Mic",    tint = TxtMuted, modifier = Modifier.size(22.dp).clickable { })
                    Icon(Icons.Outlined.Image,         "Gallery",tint = TxtMuted, modifier = Modifier.size(22.dp).clickable { })
                    Icon(Icons.Outlined.EmojiEmotions, "Emoji",  tint = TxtMuted, modifier = Modifier.size(22.dp).clickable { })
                    Icon(Icons.Outlined.AddCircle,     "More",   tint = TxtMuted, modifier = Modifier.size(22.dp).clickable { })
                }
            }
        }
    }

    // ── Options Bottom Sheet ───────────────────────────────────
    if (showOptions) {
        ModalBottomSheet(
            onDismissRequest = { showOptions = false },
            containerColor   = SheetBg,
            tonalElevation   = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Group header inside sheet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AvatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = circle?.name?.take(1)?.uppercase() ?: "G",
                            color      = TxtWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text       = circle?.name ?: "Group Chat",
                            color      = TxtWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp
                        )
                        Text(
                            text  = "${circle?.members?.size ?: 0} members",
                            color = TxtMuted,
                            fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF222430), modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))

                // ── Section 1 ──────────────────────────────────
                SheetSectionLabel("Group")

                SheetOption(
                    icon    = Icons.Default.Group,
                    iconBg  = Color(0xFF2B5FF3),
                    label   = "See chat members"
                ) {
                    showOptions = false
                    showMembers = true
                }

                SheetOption(
                    icon    = Icons.Default.Link,
                    iconBg  = Color(0xFF10B981),
                    label   = "Invite link"
                ) {
                    val link = "https://safeko.app/join/$circleId"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Invite Link", link))
                    Toast.makeText(context, "Invite link copied!", Toast.LENGTH_SHORT).show()
                    showOptions = false
                }

                SheetOption(
                    icon    = Icons.Rounded.QrCodeScanner,
                    iconBg  = Color(0xFF2B5FF3),
                    label   = "QR Code"
                ) {
                    showOptions = false
                    showQrDialog = true
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFF222430), modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))

                // ── Section 2: Privacy & Support ───────────────
                SheetSectionLabel("Privacy & Support")

                SheetOption(
                    icon    = Icons.Default.ExitToApp,
                    iconBg  = Color(0xFFF59E0B),
                    label   = "Leave chat",
                    tint    = TxtWhite
                ) {
                    Firebase.firestore.collection("circles").document(circleId)
                        .update("members", FieldValue.arrayRemove(currentUserId))
                        .addOnSuccessListener {
                            showOptions = false
                            onBack()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to leave", Toast.LENGTH_SHORT).show()
                        }
                }

                // Admin-only actions
                if (isAdmin) {
                    SheetOption(
                        icon    = Icons.Default.PersonRemove,
                        iconBg  = Color(0xFF6366F1),
                        label   = "Remove member",
                        tint    = TxtWhite
                    ) {
                        // TODO: Navigate to member removal picker
                        showOptions = false
                        showMembers = true
                    }

                    SheetOption(
                        icon    = Icons.Default.Delete,
                        iconBg  = DangerRed,
                        label   = "Delete chat",
                        tint    = DangerRed
                    ) {
                        Firebase.firestore.collection("circles").document(circleId)
                            .delete()
                            .addOnSuccessListener {
                                showOptions = false
                                onBack()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
        }
    }

    // ── QR Code Dialog ─────────────────────────────────────────
    if (showQrDialog) {
        GroupQrDialog(
            circle = circle,
            onDismiss = { showQrDialog = false }
        )
    }

    // ── Members Sheet ──────────────────────────────────────────
    if (showMembers) {
        ModalBottomSheet(
            onDismissRequest = { showMembers = false },
            containerColor   = SheetBg,
            tonalElevation   = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text     = "Members (${circle?.members?.size ?: 0})",
                    color    = TxtWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                )
                HorizontalDivider(color = Color(0xFF222430), modifier = Modifier.padding(horizontal = 16.dp))

                circle?.members?.forEach { uid ->
                                val isOwner = uid == circle?.ownerId
                                MemberRow(
                                    uid         = uid,
                                    isOwner     = isOwner,
                                    showRemove  = isAdmin && !isOwner && uid != currentUserId,
                                    onRemove    = {
                                        Firebase.firestore.collection("circles").document(circleId)
                                            .update("members", FieldValue.arrayRemove(uid))
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Member removed", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                )
                            }
            }
        }
    }
}

// ── Reusable sheet components ─────────────────────────────────

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text     = text,
        color    = TxtMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun SheetOption(
    icon: ImageVector,
    iconBg: Color,
    label: String,
    tint: Color = TxtWhite,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconBg.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = iconBg, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Member row with name lookup ───────────────────────────────

@Composable
private fun MemberRow(
    uid: String,
    isOwner: Boolean,
    showRemove: Boolean,
    onRemove: () -> Unit
) {
    var displayName by remember(uid) { mutableStateOf<String?>(null) }
    var profileUrl by remember(uid) { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        Firebase.firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                displayName = doc.getString("name")?.takeIf { it.isNotBlank() } ?: "Unknown User"
                profileUrl = doc.getString("profilePhoto")
            }
            .addOnFailureListener {
                displayName = "Unknown User"  // fallback to generic name on error
            }
    }

    val name = displayName ?: "…"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!profileUrl.isNullOrBlank()) {
            AsyncImage(
                model = profileUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier         = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AvatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = name.take(1).uppercase(),
                    color      = TxtWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = TxtWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (isOwner) Text("Admin", color = Color(0xFF2B5FF3), fontSize = 12.sp)
        }
        if (showRemove) {
            TextButton(onClick = onRemove) {
                Text("Remove", color = DangerRed, fontSize = 13.sp)
            }
        }
    }
}

// ── Message bubble ────────────────────────────────────────────

@Composable
fun ChatBubble(message: Message, isMe: Boolean) {
    var profileUrl by remember(message.senderId) { mutableStateOf(message.senderProfileUrl) }

    if (profileUrl == null && !isMe) {
        LaunchedEffect(message.senderId) {
            Firebase.firestore.collection("users").document(message.senderId).get()
                .addOnSuccessListener { doc ->
                    profileUrl = doc.getString("profilePhoto")
                }
        }
    }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            if (!profileUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profileUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .align(Alignment.Bottom),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier         = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AvatarBg)
                        .align(Alignment.Bottom),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = message.senderName?.take(1)?.uppercase() ?: "?",
                        color      = TxtWhite,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            if (!isMe && !message.senderName.isNullOrBlank()) {
                Text(
                    text       = message.senderName,
                    color      = TxtMuted,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Surface(
                color    = if (isMe) BubbleSent else BubbleRecv,
                shape    = RoundedCornerShape(
                    topStart    = if (isMe) 18.dp else 4.dp,
                    topEnd      = if (isMe) 4.dp else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd   = 18.dp
                ),
                modifier = Modifier.widthIn(max = 260.dp)
            ) {
                Text(
                    text     = message.text,
                    color    = TxtWhite,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            if (message.timestamp > 0) {
                Text(
                    text     = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                                   .format(java.util.Date(message.timestamp)),
                    color    = TxtMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        if (isMe) Spacer(modifier = Modifier.width(4.dp))
    }
}

// ── QR Code Dialog ─────────────────────────────────────────────

@Composable
fun GroupQrDialog(
    circle: Circle?,
    onDismiss: () -> Unit
) {
    if (circle == null) return
    val inviteUrl = "https://safeko.app/join/${circle.id}"
    
    // Generate QR code bitmap
    val qrBitmap = remember(inviteUrl) { generateQrBitmap(inviteUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SheetBg,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Group QR Code",
                    color = TxtWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Scan this code to join ${circle.name}",
                    color = TxtMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Failed to generate QR", color = TxtWhite)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close", color = Color(0xFF2B5FF3), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String): Bitmap? {
    return try {
        val writer = MultiFormatWriter()
        val matrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = matrix.width
        val height = matrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                // White background, black QR code
                bmp.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
