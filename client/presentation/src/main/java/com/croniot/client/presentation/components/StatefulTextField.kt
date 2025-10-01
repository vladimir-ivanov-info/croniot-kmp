package com.croniot.client.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.flow.StateFlow

@Composable
fun StatefulTextField(
    stringFlow: StateFlow<String>,
    placeholderString: String,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    val text by stringFlow.collectAsState() // Observe the current value

    TextField(
        value = text,
        onValueChange = { onValueChange(it) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholderString) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        singleLine = true,
        maxLines = 1,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,

        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            // TODO
            /*focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.LightGray,
            disabledContainerColor = Color.Gray,
            errorContainerColor = Color(0xFFFFEAEA),

            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.DarkGray,

            cursorColor = Color.Red,
            focusedLabelColor = Color.Blue,
            unfocusedLabelColor = Color.Gray,
            errorLabelColor = Color.Red*/

            // focusedLabelColor = Color.Red,
        ),
    )
}

@Composable
fun StatefulTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "",
    placeholderString: String = "",
    isPassword: Boolean = false,
    enabled: Boolean = true,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholderString) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        singleLine = true,
        maxLines = 1,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,

        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,

            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    label: String = "Password",
) {
    var passwordVisible by remember { mutableStateOf(false) }

    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val (icon, desc) = if (passwordVisible) {
                Icons.Default.Visibility to "Hide password"
            } else
                Icons.Default.VisibilityOff to "Show password"

            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = icon, contentDescription = desc)
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,

            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun UsernameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text("Nombre de usuario") },
        singleLine = true,
        isError = false, // TODO: validación si lo necesitas
        /*supportingText = {
            if (usernameError) Text("El nombre de usuario no es válido")
        },*/
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            autoCorrect = false,
            capitalization = KeyboardCapitalization.None,
        ),
        /*keyboardActions = KeyboardActions(
            onNext = { nextFocusRequester.requestFocus() }
        ),*/
        modifier = Modifier
            .fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
            errorContainerColor = Color.Transparent,
        ),
    )
}

@Composable
fun EmailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text("Email") },
        singleLine = true,
        isError = false, // TODO
        /*supportingText = {
            if (emailError) Text("Formato de email no válido")
        },*/
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            autoCorrect = false,
            capitalization = KeyboardCapitalization.None,
        ),
        /*keyboardActions = KeyboardActions(
            onNext = { passFocus.requestFocus() }
        ),*/
        modifier = Modifier
            .fillMaxWidth(),
        // .focusRequester(emailFocus)
        // colors = TextFieldDefaults.outlinedTextFieldColors() // usa los del tema
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
            errorContainerColor = Color.Transparent,
        ),
    )
}

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text("Password") },
        singleLine = true,
        isError = false, // TODO: validación real
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            autoCorrect = false,
            capitalization = KeyboardCapitalization.None,
        ),
        trailingIcon = {
            val (icon, desc) = if (passwordVisible) {
                Icons.Filled.Visibility to "Ocultar contraseña"
            } else {
                Icons.Filled.VisibilityOff to "Mostrar contraseña"
            }
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = icon, contentDescription = desc)
            }
        },
        modifier = Modifier
            .fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
            errorContainerColor = Color.Transparent,
        ),
    )
}
