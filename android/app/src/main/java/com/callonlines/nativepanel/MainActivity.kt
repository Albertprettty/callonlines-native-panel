package com.callonlines.nativepanel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callonlines.nativepanel.data.MeResponse
import com.callonlines.nativepanel.data.NetworkModule
import com.callonlines.nativepanel.data.PanelRepository
import com.callonlines.nativepanel.data.TokenStore
import com.callonlines.nativepanel.ui.theme.CallOnLinesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val tokenStore by lazy { TokenStore(this) }
    private val repo by lazy { PanelRepository(NetworkModule.panelApi(), tokenStore) }

    private val panelVm: PanelViewModel by viewModels {
        PanelViewModel.factory(repo, tokenStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CallOnLinesTheme {
                var loggedIn by remember { mutableStateOf(tokenStore.getToken() != null) }

                if (!loggedIn) {
                    LoginRoute(
                        repo = repo,
                        onLoggedIn = {
                            panelVm.clearState()
                            loggedIn = true
                        },
                    )
                } else {
                    PanelRoute(
                        vm = panelVm,
                        onLogout = {
                            tokenStore.clear()
                            loggedIn = false
                        },
                        openUrl = { url ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                    )
                }
            }
        }
    }
}

private fun copyLabel(ctx: Context, label: String, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(ctx, "Copiado", Toast.LENGTH_SHORT).show()
}

private fun sipAllText(me: MeResponse): String {
    val sip = me.sip
    val caller = sip?.callerid?.takeIf { it.isNotBlank() } ?: "-"
    return buildString {
        appendLine("SIP Username: ${sip?.username.orEmpty()}")
        appendLine("Server: ${sip?.server.orEmpty()}")
        appendLine("Port: ${sip?.port.orEmpty()}")
        appendLine("CallerID: $caller")
    }.trim()
}

@Composable
private fun LoginRoute(
    repo: PanelRepository,
    onLoggedIn: () -> Unit,
) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("CallOnLines", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Acceso clientes", style = MaterialTheme.typography.titleMedium)
        Text(
            "Ingresa tu usuario y contraseña. Luego verás el mismo panel (saldo, CallerID, SIP, recargas y clave).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("Usuario") },
            placeholder = { Text("Ej: cliente001 o @cliente001") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        err?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                err = null
                loading = true
                scope.launch {
                    val r = repo.login(user, pass)
                    loading = false
                    r.fold(
                        onSuccess = { login ->
                            tokenStore.saveToken(login.token!!)
                            Toast.makeText(ctx, "Bienvenido", Toast.LENGTH_SHORT).show()
                            onLoggedIn()
                        },
                        onFailure = { e ->
                            err = e.message ?: "Error de acceso"
                        },
                    )
                }
            },
            enabled = !loading && user.isNotBlank() && pass.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (loading) "Entrando…" else "Entrar al panel")
            }
        }
    }
}

@Composable
private fun ColumnScope.PanelBody(
    ui: PanelUiState,
    vm: PanelViewModel,
    ctx: Context,
    onLogout: () -> Unit,
    openUrl: (String) -> Unit,
) {
    ui.error?.let {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(it, Modifier.padding(12.dp))
        }
        Spacer(Modifier.height(8.dp))
    }

    ui.message?.let {
        Card {
            Text(it, Modifier.padding(12.dp))
        }
        Spacer(Modifier.height(8.dp))
    }

    val me = ui.me ?: return

    if (me.lowBalance == true) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Saldo bajo", fontWeight = FontWeight.Bold)
                Text("Recarga para evitar cortes en el servicio.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Saldo actual", style = MaterialTheme.typography.titleMedium)
            Text(
                "$${me.balanceFormatted ?: "—"} USD",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Acciones rápidas", style = MaterialTheme.typography.titleMedium)
            Text(
                "Recarga y copia de datos SIP (como en el panel web).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(12.dp))
            val links = me.links
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                links?.rechargeWeb?.let { url ->
                    Button(onClick = { openUrl(url) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Recargar (web)")
                    }
                }
                links?.rechargeWhatsapp?.let { url ->
                    OutlinedButton(onClick = { openUrl(url) }, modifier = Modifier.fillMaxWidth()) {
                        Text("WhatsApp recarga")
                    }
                }
                OutlinedButton(
                    onClick = { copyLabel(ctx, "SIP", sipAllText(me)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Copiar TODO SIP")
                }
                OutlinedButton(
                    onClick = {
                        copyLabel(ctx, "Usuario SIP", me.sip?.username.orEmpty())
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Copiar usuario SIP")
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("CallerID", style = MaterialTheme.typography.titleMedium)
            Text(
                "Número que se muestra en llamadas salientes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.callerDraft,
                onValueChange = vm::setCallerDraft,
                label = { Text("Nuevo CallerID (6–16 dígitos)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { vm.saveCallerId { } },
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Actualizar CallerID")
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Detalles SIP", style = MaterialTheme.typography.titleMedium)
            val sip = me.sip
            Text("Usuario: ${sip?.username.orEmpty()}")
            Text("Servidor: ${sip?.server.orEmpty()}")
            Text("Puerto: ${sip?.port.orEmpty()}")
            Text("CallerID: ${sip?.callerid.orEmpty().ifBlank { "—" }}")
        }
    }

    Spacer(Modifier.height(12.dp))

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Cambiar contraseña", style = MaterialTheme.typography.titleMedium)
            Text(
                "Actualiza la clave del usuario y el SIP. Deja nueva vacía para generar una automática (cerrará sesión).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.passCurrent,
                onValueChange = vm::setPassCurrent,
                label = { Text("Contraseña actual") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.passNew,
                onValueChange = vm::setPassNew,
                label = { Text("Nueva (opcional)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.passConfirm,
                onValueChange = vm::setPassConfirm,
                label = { Text("Confirmar nueva") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    vm.changePassword {
                        onLogout()
                    }
                },
                enabled = !ui.loading && ui.passCurrent.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Actualizar contraseña")
            }
        }
    }
}

@Composable
private fun PanelRoute(
    vm: PanelViewModel,
    onLogout: () -> Unit,
    openUrl: (String) -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        vm.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Panel del cliente", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                ui.me?.user?.display?.let { handle ->
                    Text(handle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            TextButton(onClick = { vm.refresh() }) {
                Text("Actualizar")
            }
        }
        TextButton(onClick = onLogout, modifier = Modifier.align(Alignment.End)) {
            Text("Cerrar sesión")
        }

        if (ui.loading && ui.me == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else {
            PanelBody(ui, vm, ctx, onLogout, openUrl)
        }

        if (ui.loading && ui.me != null) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
