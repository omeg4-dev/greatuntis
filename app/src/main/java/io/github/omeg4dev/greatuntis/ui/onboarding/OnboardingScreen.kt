package io.github.omeg4dev.greatuntis.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.omeg4dev.greatuntis.R
import io.github.omeg4dev.greatuntis.model.Account
import io.github.omeg4dev.greatuntis.model.SchoolInfo
import io.github.omeg4dev.greatuntis.ui.fake.FakeData
import io.github.omeg4dev.greatuntis.ui.theme.GreatUntisTheme
import io.github.omeg4dev.greatuntis.ui.theme.tabularNumbers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.concurrent.Executors

enum class OnboardingStep {
    WELCOME,
    SCANNER,
    MANUAL,
    CONFIRM,
}

@Composable
fun OnboardingScreen(
    parseAccount: (String) -> Account? = { FakeData.fakeParseAccount(it) },
    onLogin: suspend (Account) -> Result<Unit> = { Result.success(Unit) },
    searchSchools: suspend (String) -> Result<List<SchoolInfo>> = { FakeData.sampleSchools.let { Result.success(it) } },
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var pendingAccount by remember { mutableStateOf<Account?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val notUntisQrMessage = stringResource(R.string.not_a_untis_qr)

    // Image Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val account = decodeAccountFromUri(context, uri, parseAccount)
                if (account != null) {
                    pendingAccount = account
                    step = OnboardingStep.CONFIRM
                } else {
                    snackbarHostState.showSnackbar(notUntisQrMessage)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (step) {
                OnboardingStep.WELCOME -> {
                    WelcomeContent(
                        onScanClick = { step = OnboardingStep.SCANNER },
                        onPickImageClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onManualClick = { step = OnboardingStep.MANUAL },
                    )
                }
                OnboardingStep.SCANNER -> {
                    ScannerContent(
                        parseAccount = parseAccount,
                        onAccountScanned = { acc ->
                            pendingAccount = acc
                            step = OnboardingStep.CONFIRM
                        },
                        onInvalidQr = {
                            scope.launch {
                                snackbarHostState.showSnackbar(notUntisQrMessage)
                            }
                        },
                        onBack = { step = OnboardingStep.WELCOME },
                    )
                }
                OnboardingStep.MANUAL -> {
                    ManualInputContent(
                        searchSchools = searchSchools,
                        parseAccount = parseAccount,
                        onAccountReady = { acc ->
                            pendingAccount = acc
                            step = OnboardingStep.CONFIRM
                        },
                        onBack = { step = OnboardingStep.WELCOME },
                    )
                }
                OnboardingStep.CONFIRM -> {
                    if (pendingAccount != null) {
                        ConfirmLoginContent(
                            account = pendingAccount!!,
                            onLogin = onLogin,
                            onBack = { step = OnboardingStep.WELCOME },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WelcomeContent(
    onScanClick: () -> Unit,
    onPickImageClick: () -> Unit,
    onManualClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(140.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(72.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.pitch),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OnboardingStepCard(
                stepNumber = 1,
                title = stringResource(R.string.onboarding_step_1_title),
                description = stringResource(R.string.onboarding_step_1_desc),
            )
            OnboardingStepCard(
                stepNumber = 2,
                title = stringResource(R.string.onboarding_step_2_title),
                description = stringResource(R.string.onboarding_step_2_desc),
            )
            OnboardingStepCard(
                stepNumber = 3,
                title = stringResource(R.string.onboarding_step_3_title),
                description = stringResource(R.string.onboarding_step_3_desc),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onScanClick,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonDefaults.LargeContainerHeight),
            ) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.btn_scan_qr))
            }

            FilledTonalButton(
                onClick = onPickImageClick,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonDefaults.LargeContainerHeight),
            ) {
                Icon(Icons.Outlined.QrCode, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.btn_pick_image))
            }

            TextButton(
                onClick = onManualClick,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(text = stringResource(R.string.btn_manual_input))
            }
        }
    }
}

@Composable
private fun OnboardingStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.titleSmall.tabularNumbers(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ScannerContent(
    parseAccount: (String) -> Account?,
    onAccountScanned: (Account) -> Unit,
    onInvalidQr: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            var lastScannedTime by remember { mutableStateOf(0L) }

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val executor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = CameraPreview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            val now = System.currentTimeMillis()
                            if (now - lastScannedTime > 1500) {
                                val buffer = imageProxy.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                val rowStride = imageProxy.planes[0].rowStride

                                val text = QrDecoder.decodeYuv(
                                    bytes,
                                    imageProxy.width,
                                    imageProxy.height,
                                    rowStride,
                                )

                                if (!text.isNullOrEmpty()) {
                                    lastScannedTime = now
                                    val acc = parseAccount(text)
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        if (acc != null) {
                                            onAccountScanned(acc)
                                        } else {
                                            onInvalidQr()
                                        }
                                    }
                                }
                            }
                            imageProxy.close()
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis,
                            )
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Square Viewfinder overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
            ) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent),
                ) {
                    Surface(
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {}
                }
            }
        } else {
            // Permission denied UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.camera_permission_required),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val intent = Intent(
                            AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                        context.startActivity(intent)
                    },
                ) {
                    Text(text = stringResource(R.string.btn_open_settings))
                }
            }
        }

        // Back button top left
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.btn_back),
                tint = Color.White,
            )
        }
    }
}

@Composable
fun ManualInputContent(
    searchSchools: suspend (String) -> Result<List<SchoolInfo>>,
    parseAccount: (String) -> Account?,
    onAccountReady: (Account) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    // Name of the school the user picked; typing it into the field must not start a new search.
    var pickedName by remember { mutableStateOf<String?>(null) }
    var searchResults by remember { mutableStateOf<List<SchoolInfo>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    var host by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var showServerFields by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Debounced school search (400 ms)
    LaunchedEffect(searchQuery) {
        if (searchQuery == pickedName) {
            searchResults = emptyList()
        } else if (searchQuery.trim().length >= 2) {
            delay(400)
            isSearching = true
            searchSchools(searchQuery.trim()).onSuccess {
                searchResults = it
                isSearching = false
            }.onFailure {
                searchResults = emptyList()
                isSearching = false
            }
        } else {
            searchResults = emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back),
                )
            }
            Text(
                text = stringResource(R.string.btn_manual_input),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search schools field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(stringResource(R.string.search_schools_label)) },
            placeholder = { Text(stringResource(R.string.search_schools_placeholder)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        // Search results
        if (searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            searchResults.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            host = item.host
                            school = item.loginName
                            pickedName = item.displayName
                            searchQuery = item.displayName
                            searchResults = emptyList()
                        },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = item.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Expandable Server + Schulname toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showServerFields = !showServerFields }
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.manual_server_details),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (showServerFields) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        AnimatedVisibility(visible = showServerFields || host.isNotEmpty() || school.isNotEmpty()) {
            Column {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.server_host_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
                OutlinedTextField(
                    value = school,
                    onValueChange = { school = it },
                    label = { Text(stringResource(R.string.school_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Username
        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text(stringResource(R.string.username_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Secret key (password field with toggle)
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text(stringResource(R.string.key_label)) },
            singleLine = true,
            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                    Icon(
                        imageVector = if (isKeyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = null,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // "Text einfügen" button
        OutlinedButton(
            onClick = {
                val clipText = clipboardManager.getText()?.text
                if (!clipText.isNullOrBlank()) {
                    val acc = parseAccount(clipText)
                    if (acc != null) {
                        host = acc.host
                        school = acc.school
                        user = acc.user
                        key = acc.key
                    } else {
                        key = clipText.trim()
                    }
                }
            },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(Icons.Outlined.ContentPaste, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_paste_text))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weiter button
        val canContinue = host.isNotBlank() && school.isNotBlank() && user.isNotBlank() && key.isNotBlank()
        Button(
            onClick = {
                if (canContinue) {
                    onAccountReady(
                        Account(
                            host = host.trim(),
                            school = school.trim(),
                            user = user.trim(),
                            key = key.trim(),
                        ),
                    )
                }
            },
            enabled = canContinue,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonDefaults.LargeContainerHeight),
        ) {
            Text(stringResource(R.string.btn_continue))
        }
    }
}

@Composable
fun ConfirmLoginContent(
    account: Account,
    onLogin: suspend (Account) -> Result<Unit>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val maskedKey = if (account.key.length >= 4) {
        account.key.take(4) + "••••••••"
    } else {
        "••••••••••••"
    }

    val credErrorMsg = stringResource(R.string.error_credentials)
    val networkErrorMsg = stringResource(R.string.error_network)
    val genericErrorPattern = stringResource(R.string.error_generic, "")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.btn_back),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.confirm_login_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(
                        text = stringResource(R.string.school_name_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = account.school,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.username_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = account.user,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.key_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = maskedKey,
                        style = MaterialTheme.typography.titleMedium.tabularNumbers(),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    onLogin(account).onSuccess {
                        isLoading = false
                    }.onFailure { err ->
                        isLoading = false
                        val msg = err.message ?: ""
                        errorMessage = when {
                            msg.contains("Benutzer oder Schlüssel falsch", ignoreCase = true) ||
                                msg.contains("credentials", ignoreCase = true) -> credErrorMsg
                            msg.contains("verbindung", ignoreCase = true) ||
                                msg.contains("network", ignoreCase = true) -> networkErrorMsg
                            else -> genericErrorPattern + msg
                        }
                    }
                }
            },
            enabled = !isLoading,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonDefaults.LargeContainerHeight),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.logging_in))
            } else {
                Text(text = stringResource(R.string.btn_login))
            }
        }
    }
}

private fun decodeAccountFromUri(
    context: Context,
    uri: Uri,
    parseAccount: (String) -> Account?,
): Account? {
    return runCatching {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null

        // Downscale to <= 1600 px long side if needed
        val maxDim = maxOf(bitmap.width, bitmap.height)
        val scaledBitmap = if (maxDim > 1600) {
            val scale = 1600f / maxDim
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true,
            )
        } else {
            bitmap
        }

        val w = scaledBitmap.width
        val h = scaledBitmap.height
        val pixels = IntArray(w * h)
        scaledBitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val qrText = QrDecoder.decodeQr(pixels, w, h) ?: return null
        parseAccount(qrText)
    }.getOrNull()
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    GreatUntisTheme {
        OnboardingScreen()
    }
}
