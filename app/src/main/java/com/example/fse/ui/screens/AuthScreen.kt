package com.example.fse.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fse.data.auth.FatSecretOAuth1Flow
import com.example.fse.di.AppContainer
import kotlinx.coroutines.launch
import androidx.browser.customtabs.CustomTabsIntent

@Composable
fun AuthScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasTokens by container.oauth1TokenStore.hasTokens.collectAsState(initial = false)
    var verifier by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var requestTokenResult by remember { mutableStateOf<FatSecretOAuth1Flow.RequestTokenResult?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "FatSecret Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (hasTokens) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Connected", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Your diary, favorites and recently eaten are synced with FatSecret.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                container.oauth1TokenStore.clearTokens()
                                isLoading = false
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        else Text("Disconnect")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Connect your existing FatSecret account to sync diary, favorites and recently eaten.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (requestTokenResult == null) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    container.oauth1Flow.getRequestToken()
                                        .onSuccess { result ->
                                            requestTokenResult = result
                                            val tabsIntent = CustomTabsIntent.Builder().build()
                                            tabsIntent.launchUrl(context, Uri.parse(result.authorizeUrl))
                                        }
                                        .onFailure { errorMessage = it.message }
                                    isLoading = false
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                            else Text("Connect FatSecret")
                        }
                    } else {
                        Text(
                            "Authorize in the browser, then paste the verification code below:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = verifier,
                            onValueChange = { verifier = it },
                            label = { Text("Verification code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    if (verifier.isBlank()) {
                                        errorMessage = "Enter the verification code"
                                        return@launch
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    val result = requestTokenResult!!
                                    container.oauth1Flow.exchangeForAccessToken(
                                        result.requestToken,
                                        result.requestTokenSecret,
                                        verifier
                                    )
                                        .onSuccess {
                                            requestTokenResult = null
                                            verifier = ""
                                        }
                                        .onFailure { errorMessage = it.message }
                                    isLoading = false
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                            else Text("Complete")
                        }
                        Button(
                            onClick = { requestTokenResult = null; verifier = "" },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(msg, color = MaterialTheme.colorScheme.error)
        }
    }
}
