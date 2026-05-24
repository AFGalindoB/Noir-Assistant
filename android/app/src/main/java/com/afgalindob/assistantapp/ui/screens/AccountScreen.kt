@file:Suppress("AssignedValueIsNeverRead")

package com.afgalindob.assistantapp.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.data.local.preferences.UserPreferences
import com.afgalindob.assistantapp.ui.components.bottomsheet.ProfileImageSheet
import com.afgalindob.assistantapp.ui.dialogs.dialog.FullScreenImageRow
import com.afgalindob.assistantapp.ui.theme.AccentSecondary
import com.afgalindob.assistantapp.ui.theme.OnAccentSecondary
import com.afgalindob.assistantapp.ui.theme.OnSurfacePrimary
import com.afgalindob.assistantapp.ui.theme.SurfaceVariant
import com.afgalindob.assistantapp.viewmodel.SettingsViewModel
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.afgalindob.assistantapp.ui.components.NoirBackground
import com.afgalindob.assistantapp.ui.components.bottomsheet.LanguageSelectorSheet
import com.afgalindob.assistantapp.ui.components.bottomsheet.TimeReminderSheet
import com.afgalindob.assistantapp.ui.dialogs.alert.GetServerUsernameDialog
import com.afgalindob.assistantapp.ui.dialogs.dialog.AdjustProfileImage
import com.afgalindob.assistantapp.ui.dialogs.dialog.QrScannerDialog
import com.afgalindob.assistantapp.data.local.network.ServerStatus

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AccountScreen(
    viewModel: SettingsViewModel,
    onRendered: () -> Unit
) {

    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()

    var showScanner by remember { mutableStateOf(false) }

    // ── Estados de control de UI ──────────────────────────────────────────
    var isEditing by remember { mutableStateOf(false) }
    var isViewerOpen by remember { mutableStateOf(false) }
    var showEditImageSheet by remember { mutableStateOf(false) }
    var showEditLanguageSheet by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedUriForEdit by remember { mutableStateOf<String?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showSetUsernameDialog by remember { mutableStateOf(false) }

    // ── Estados temporales (Borrador) ─────────────────────────────────────
    var tempName by remember(prefs) { mutableStateOf(prefs.name) }
    var tempServerUsername by remember(prefs) { mutableStateOf(prefs.serverUsername) }
    var tempBio by remember(prefs) { mutableStateOf(prefs.bio) }
    var tempImageUri by remember(prefs) { mutableStateOf(prefs.imageUri) }
    var tempX by remember(prefs) { mutableFloatStateOf(prefs.centerX) }
    var tempY by remember(prefs) { mutableFloatStateOf(prefs.centerY) }
    var tempZoom by remember(prefs) { mutableFloatStateOf(prefs.zoom) }
    var tempReminderTime by remember(prefs) { mutableStateOf(prefs.reminderTime) }

    var imageSize by remember { mutableStateOf(Size.Zero) }

    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // ── Lógica de Selección de Imagen ─────────────────────────────────────
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedUriForEdit = it.toString()
                showEditDialog = true
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScanner = true
        }
    }

    val isCameraPermissionGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    // ── Lógica de Inicio de la Pantalla ─────────────────────────────────────
    LaunchedEffect(Unit) {
        repeat(2) { withFrameNanos { } }
        onRendered()
    }

    NoirBackground(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Column(modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // ── Visualizador de Imagen Local ────────────────────────────────
                Box(contentAlignment = Alignment.BottomEnd) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(SurfaceVariant.copy(alpha = 0.2f))
                            .clickable {
                                if (isEditing) showEditImageSheet = true
                                else isViewerOpen = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val viewSize = constraints.maxWidth.toFloat()

                        if (tempImageUri != null) {
                            AsyncImage(
                                model = tempImageUri,
                                contentDescription = null,
                                onSuccess = { state ->
                                    imageSize = Size(
                                        state.painter.intrinsicSize.width,
                                        state.painter.intrinsicSize.height
                                    )
                                },
                                modifier = Modifier
                                    .wrapContentSize(unbounded = true, align = Alignment.TopStart)
                                    .graphicsLayer {
                                        if (imageSize != Size.Zero && viewSize > 0f) {
                                            val baseDim = minOf(imageSize.width, imageSize.height)

                                            val finalScale = (viewSize / baseDim) * tempZoom

                                            scaleX = finalScale
                                            scaleY = finalScale

                                            translationX = (viewSize / 2f) - (tempX * imageSize.width * finalScale)
                                            translationY = (viewSize / 2f) - (tempY * imageSize.height * finalScale)

                                            transformOrigin = TransformOrigin(0f, 0f)
                                        }
                                    },
                                contentScale = ContentScale.None
                            )
                        } else {
                            // Fallback solo cuando genuinamente no hay imagen
                            Icon(
                                painter = painterResource(R.drawable.account),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(0.5f),
                                tint = OnAccentSecondary
                            )
                        }
                    }
                    if (isEditing) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AccentSecondary)
                                .clickable { showEditImageSheet = true }
                                .padding(8.dp)
                            ,
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.edit),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                tint = OnAccentSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Content ────────────────────────────────────────────────
                AnimatedContent(
                    targetState = isEditing,
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> width } + fadeOut())
                        }.using(
                            SizeTransform(clip = false)
                        )
                    },
                    label = "HorizontalModeTransition"
                ) { editing ->
                    if (editing) {
                        EditModeContent(
                            name = tempName,
                            onNameChange = { tempName = it },
                            bio = tempBio,
                            onBioChange = { tempBio = it },
                            username = tempServerUsername,
                            onUsernameClick = { showSetUsernameDialog = true },
                            focusManager = focusManager
                        )
                    } else {
                        ViewModeContent(
                            name = tempName,
                            username = tempServerUsername,
                            bio = tempBio,
                            language = prefs.language,
                            reminderTime = tempReminderTime,
                            serverStatus = serverStatus,
                            onAlarmClick = { showTimePicker = true },
                            onLanguageClick = { showEditLanguageSheet = true },
                            onServerClick = {
                                if (serverStatus == ServerStatus.NAME_REQUIRED){
                                    showSetUsernameDialog = true
                                }else{
                                    val requiresScanner =
                                        serverStatus == ServerStatus.UNLINKED ||
                                                serverStatus == ServerStatus.READY_TO_CONNECT ||
                                                serverStatus == ServerStatus.AWAITING_APPROVAL

                                    if (requiresScanner) {
                                        if (isCameraPermissionGranted) {
                                            showScanner = true
                                        } else {
                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    } else {
                                        viewModel.checkCommunicationWithServer()
                                    }
                                }
                            }
                        )
                    }
                }
            }


            // ── Barra Inferior de Acciones ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (isEditing) {
                    // Modo Edición

                    // -- Boton Cancelar --
                    Button(
                        onClick = {
                            tempName = prefs.name
                            tempServerUsername = prefs.serverUsername
                            tempBio = prefs.bio
                            tempImageUri = prefs.imageUri
                            tempX = prefs.centerX
                            tempY = prefs.centerY
                            tempZoom = prefs.zoom
                            isEditing = false
                            focusManager.clearFocus()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceVariant.copy(alpha = 0.3f),
                            contentColor = OnSurfacePrimary
                        ),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) { Text(stringResource(R.string.cancel)) }

                    // -- Boton Guardar --
                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                UserPreferences(
                                    name = tempName,
                                    bio = tempBio,
                                    imageUri = tempImageUri,
                                    centerX = tempX,
                                    centerY = tempY,
                                    zoom = tempZoom
                                )
                            )
                            isEditing = false
                            focusManager.clearFocus()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentSecondary,
                            contentColor = OnAccentSecondary
                        ),
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    ) { Text(stringResource(R.string.save)) }
                } else {
                    // Modo Visualización

                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentSecondary,
                            contentColor = OnAccentSecondary
                        )
                    ) {
                        Icon(painterResource(R.drawable.edit), null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.edit) + " " + stringResource(R.string.profile))
                    }
                }
            }
        }
    }

    // ── Overlays Imagen ─────────────────────────────────────────────────────

    // Visulizar Imagen a Pantalla Completa
    if (isViewerOpen) {
        FullScreenImageRow(
            imageUri = tempImageUri,
            onDismiss = { isViewerOpen = false }
        )
    }

    // Menu Editar Foto
    ProfileImageSheet(
        showSheet = showEditImageSheet,
        hasImage = tempImageUri != null,
        onDismiss = { showEditImageSheet = false },
        onPickImage = {
            showEditImageSheet = false
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onEditExisting = {
            showEditImageSheet = false
            selectedUriForEdit = tempImageUri // Pasamos la URI actual
            showEditDialog = true
        },
        onRemoveImage = {
            showEditImageSheet = false
            tempImageUri = null
            tempX = 0.5f; tempY = 0.5f; tempZoom = 1f
        }
    )

    // Ajustar Imagen
    if (showEditDialog) {
        AdjustProfileImage(
            imageUri = selectedUriForEdit,
            initialX = tempX,
            initialY = tempY,
            initialZoom = tempZoom,
            onDismiss = { showEditDialog = false; selectedUriForEdit = null },
            onConfirm = { uri, x, y, z ->
                tempImageUri = uri
                tempX = x; tempY = y; tempZoom = z
                showEditDialog = false; selectedUriForEdit = null
            }
        )
    }

    // ── Overlay Lenguaje ─────────────────────────────────────────────────────

    if (showEditLanguageSheet) {
        LanguageSelectorSheet(
            onDismiss = { showEditLanguageSheet = false },
            onLanguageSelected = {
                viewModel.updateLanguage(it)
                showEditLanguageSheet = false
            }
        )
    }

    // ── Overlay Recordatorios ───────────────────────────────────────────────

    if (showTimePicker) {
        TimeReminderSheet(
            initialTime = tempReminderTime,
            onDismiss = { showTimePicker = false },
            onTimeSelected = {
                viewModel.updateReminderTime(it)
                tempReminderTime = it
                showTimePicker = false
            }
        )
    }

    // ── Overlay Server ──────────────────────────────────────────────────

    // Set Username
    if (showSetUsernameDialog) {
        GetServerUsernameDialog(
            initialValue = tempServerUsername,
            onDismiss = { showSetUsernameDialog = false },
            onConfirm = {
                viewModel.updateServerUsername(it)
                showSetUsernameDialog = false
            }
        )
    }

    // QR Scanner
    if (showScanner) {
        QrScannerDialog(
            onDismiss = { showScanner = false },
            onCodeDetected = { qrData ->
                val requiresAuth =
                    serverStatus == ServerStatus.UNLINKED ||
                    serverStatus == ServerStatus.READY_TO_CONNECT
                if (requiresAuth) {
                    viewModel.requestAuth(qrData)
                } else {
                    viewModel.requestToken(qrData)
                }
                showScanner = false
            }
        )
    }
}

@Composable
private fun ViewModeContent(
    name: String,
    username: String,
    bio: String,
    language: String,
    reminderTime: String,
    serverStatus: ServerStatus,
    onLanguageClick: () -> Unit,
    onAlarmClick: () -> Unit,
    onServerClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        // Sección Nombre
        Text(
            text = name.ifBlank { stringResource(R.string.name) },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = if (name.isNotBlank()) OnSurfacePrimary else OnSurfacePrimary.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        // Sección Nombre de Usuario
        if (username.isNotBlank()){
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "@$username",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfacePrimary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
        }else{
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Sección Bio
        Text(
            text = bio.ifBlank { stringResource(R.string.biograpy) },
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            color = if (name.isNotBlank()) OnSurfacePrimary else OnSurfacePrimary.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyMedium
        )

        // ── Sección Accesibilidad ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.accessibility),
                style = MaterialTheme.typography.bodyMedium,
                color = SurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            HorizontalDivider(color = SurfaceVariant, thickness = 1.dp, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Sección Idiomas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable { onLanguageClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.language),
                contentDescription = "Change Language",
                tint = SurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${stringResource(R.string.language)}: ${language.uppercase()}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Sección Alarma
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable { onAlarmClick() } // Lanzar el picker
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter =  painterResource(R.drawable.clock),
                contentDescription = "Scheduled Reminder" ,
                tint = SurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reminder_time),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = reminderTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // ── Sección Servidor ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.server),
                style = MaterialTheme.typography.bodyMedium,
                color = SurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            HorizontalDivider(color = SurfaceVariant, thickness = 1.dp, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Sección Conexión con Servidor

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable { onServerClick() }
                .padding(12.dp)
        ) {
            Column{
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Icono de conexión
                    if (serverStatus != ServerStatus.ONLINE && serverStatus != ServerStatus.DISCONNECTED){
                        Icon(
                            painter = when (serverStatus) {
                                ServerStatus.NAME_REQUIRED -> painterResource(R.drawable.account)
                                ServerStatus.UNLINKED -> painterResource(R.drawable.qr_scanner)
                                else -> painterResource(R.drawable.login)
                            },
                            contentDescription = "Connect to Server",
                            tint = SurfaceVariant
                        )
                    }else{
                        Icon(
                            painter = painterResource(R.drawable.circle_filled),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (serverStatus == ServerStatus.ONLINE) Color.Green else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Mensaje de conexión
                    Text(
                        text = stringResource(
                            when (serverStatus) {
                                ServerStatus.NAME_REQUIRED -> R.string.name_required
                                ServerStatus.UNLINKED -> R.string.connect_to_server
                                ServerStatus.ONLINE -> R.string.connected_to_server
                                ServerStatus.DISCONNECTED -> R.string.offline
                                ServerStatus.READY_TO_CONNECT -> R.string.ready_to_connect
                                ServerStatus.AWAITING_APPROVAL -> R.string.click_here_to_login
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EditModeContent(
    name: String,
    onNameChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    username: String,
    onUsernameClick: () -> Unit,
    focusManager: FocusManager
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUsernameClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter =  painterResource(R.drawable.account),
                contentDescription = "Scheduled Reminder" ,
                tint = SurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.username) + ": " + "@$username",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Words
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = bio,
            onValueChange = onBioChange,
            label = { Text(stringResource(R.string.biograpy)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}