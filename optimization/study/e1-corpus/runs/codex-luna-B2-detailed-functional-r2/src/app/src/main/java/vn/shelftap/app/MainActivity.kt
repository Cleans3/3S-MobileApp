package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

sealed interface Screen { data object SignIn: Screen; data object Profile: Screen; data object ChangePassword: Screen }
class MainViewModel(private val api: Api, private val store: SessionStore): ViewModel() {
    var screen by mutableStateOf<Screen>(Screen.SignIn); var member by mutableStateOf<Member?>(null); var error by mutableStateOf<String?>(null); var busy by mutableStateOf(false); var retryAfter by mutableStateOf<Long?>(null)
    init { viewModelScope.launch { val saved=store.session(); if (saved!=null) { screen=if (saved.mustChangePassword) Screen.ChangePassword else Screen.Profile; try { member=api.me() } catch (_:Exception) { signOutLocal() } } } }
    fun login(email:String,password:String) { submit { val s=api.login(email,password); store.save(s); if(s.mustChangePassword) screen=Screen.ChangePassword else { member=api.me(); screen=Screen.Profile } } }
    fun change(current:String,new:String) { submit { api.changePassword(current,new); member=api.me(); screen=Screen.Profile } }
    fun signOut() { submit { api.logout(); screen=Screen.SignIn; member=null } }
    private fun signOutLocal() { viewModelScope.launch { store.clear(); screen=Screen.SignIn } }
    private fun submit(block:suspend()->Unit) { viewModelScope.launch { busy=true; error=null; retryAfter=null; try { block() } catch(e:ApiException) { if(e.code=="TOKEN_INVALID") { signOutLocal(); return@launch }; error=when(e.code) { "INVALID_CREDENTIALS"->"Email or password is incorrect."; "ACCOUNT_LOCKED"->"Your account is locked. Please contact the library."; "RATE_LIMITED"->{retryAfter=e.retryAfter; "Too many attempts. Please try again later."}; "PASSWORD_CHANGE_REQUIRED"->"You must change your password before continuing."; else->e.message ?: "Something went wrong."} } catch(_:Exception) { error="Unable to connect. Please try again." } finally { busy=false } } }
}
class MainActivity: ComponentActivity() { override fun onCreate(b:Bundle?) { super.onCreate(b); setContent { ShelfTapTheme { val vm:MainViewModel = viewModel { MainViewModel(Api(SessionStore(applicationContext)),SessionStore(applicationContext)) }; Surface(Modifier.fillMaxSize()) { when(vm.screen) { Screen.SignIn->SignIn(vm); Screen.Profile->Profile(vm); Screen.ChangePassword->ChangePassword(vm) } } } } } }

@Composable private fun FormColumn(content:@Composable ColumnScope.()->Unit)=Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement=Arrangement.Center, content=content)
@Composable private fun ErrorText(vm:MainViewModel) { vm.error?.let { Text(it, color=MaterialTheme.colorScheme.error, modifier=Modifier.padding(top=12.dp)) }; vm.retryAfter?.let { Text("Retry in ${it}s", color=MaterialTheme.colorScheme.error) } }
@Composable private fun SignIn(vm:MainViewModel) { var email by remember{mutableStateOf("")}; var pass by remember{mutableStateOf("")}; FormColumn { Text("ShelfTap",style=MaterialTheme.typography.headlineLarge); Text("Member account",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary); Spacer(Modifier.height(32.dp)); OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,enabled=!vm.busy,modifier=Modifier.fillMaxWidth()); Spacer(Modifier.height(12.dp)); OutlinedTextField(pass,{pass=it},label={Text("Password")},singleLine=true,enabled=!vm.busy,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth()); ErrorText(vm); Spacer(Modifier.height(20.dp)); Button({vm.login(email.trim(),pass)},enabled=email.isNotBlank()&&pass.isNotBlank()&&!vm.busy,modifier=Modifier.fillMaxWidth()) { if(vm.busy) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Sign in") } } }
@Composable private fun Profile(vm:MainViewModel) { val m=vm.member ?: return; FormColumn { Text("Profile",style=MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(28.dp)); Info("Full name",m.fullName); Info("Email",m.email); Info("Member type",m.memberType); Info("Status",m.status); ErrorText(vm); Spacer(Modifier.height(28.dp)); OutlinedButton({vm.signOut()},enabled=!vm.busy,modifier=Modifier.fillMaxWidth()){Text("Sign out")} } }
@Composable private fun Info(label:String,value:String) { Column(Modifier.padding(vertical=8.dp)){Text(label,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary); Text(value,style=MaterialTheme.typography.bodyLarge)} }
@Composable private fun ChangePassword(vm:MainViewModel) { var current by remember{mutableStateOf("")}; var new by remember{mutableStateOf("")}; var confirm by remember{mutableStateOf("")}; FormColumn { Text("Change password",style=MaterialTheme.typography.headlineMedium); Text("Choose a new password to continue.",modifier=Modifier.padding(top=8.dp)); Spacer(Modifier.height(24.dp)); PField("Current password",current,{current=it},vm); PField("New password",new,{new=it},vm); PField("Confirm new password",confirm,{confirm=it},vm); if(confirm.isNotEmpty()&&new!=confirm) Text("Passwords do not match.",color=MaterialTheme.colorScheme.error); ErrorText(vm); Spacer(Modifier.height(16.dp)); Button({vm.change(current,new)},enabled=current.isNotBlank()&&new.isNotBlank()&&new==confirm&&!vm.busy,modifier=Modifier.fillMaxWidth()){if(vm.busy)CircularProgressIndicator(Modifier.size(20.dp))else Text("Save password")} } }
@Composable private fun PField(label:String,value:String,onChange:(String)->Unit,vm:MainViewModel)=OutlinedTextField(value,onChange,label={Text(label)},singleLine=true,enabled=!vm.busy,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth().padding(bottom=12.dp))
@Composable private fun ShelfTapTheme(content:@Composable()->Unit) { MaterialTheme(colorScheme=lightColorScheme(primary=androidx.compose.ui.graphics.Color(0xFF345D9D)),content=content) }
