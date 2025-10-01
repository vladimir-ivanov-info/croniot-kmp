package com.croniot.android.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Preview(showBackground = true)
@Composable
fun CroniotDashboardPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF2ECC71),      // Verde esmeralda
            secondary = Color(0xFF00BCD4),    // Azul eléctrico
            background = Color(0xFF0F1115),   // Fondo casi negro
            surface = Color(0xFF1C1E24),      // Cards oscuras
            error = Color(0xFFE53935),        // Rojo sobrio
            onPrimary = Color(0xFF0F1115),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        CroniotDashboardContent()
    }
}

@Composable
fun CroniotDashboardContent(){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Croniot Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Card de un sensor
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Battery level", fontWeight = FontWeight.Bold)
                    Text("84 %", color = MaterialTheme.colorScheme.primary)
                }
            }

            // Card de otro sensor
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("WiFi Signal", fontWeight = FontWeight.Bold)
                    Text("-67 dBm", color = MaterialTheme.colorScheme.secondary)
                }
            }

            // Botón principal
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Watering")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CroniotDashboardPreview1() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7C4DFF),      // púrpura
            secondary = Color(0xFF40C4FF),    // azul eléctrico
            background = Color(0xFF0B0E11),   // fondo casi negro
            surface = Color(0xFF1A1C20),
            error = Color(0xFFE53935),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        CroniotDashboardContent()
    }
}

@Preview(showBackground = true)
@Composable
fun CroniotDashboardPreview2() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF8BC34A),      // verde oliva
            secondary = Color(0xFFFFC107),    // dorado
            background = Color(0xFFF9F9F6),   // gris cálido claro
            surface = Color(0xFFFFFFFF),
            error = Color(0xFFE53935),
            onPrimary = Color(0xFF212121),
            onBackground = Color(0xFF212121),
            onSurface = Color(0xFF212121)
        )
    ) {
        CroniotDashboardContent()
    }
}

@Preview(showBackground = true)
@Composable
fun CroniotDashboardPreview3() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFFD700),      // dorado
            secondary = Color(0xFFC0C0C0),    // plateado
            background = Color(0xFF0D0D0D),   // negro profundo
            surface = Color(0xFF1A1A1A),
            error = Color(0xFFE53935),
            onPrimary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        CroniotDashboardContent()
    }
}

@Preview(showBackground = true)
@Composable
fun CroniotDashboardPreview4() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF00E5FF),      // cian brillante
            secondary = Color(0xFF009688),    // verde azulado
            background = Color(0xFFF4F9FC),   // blanco hielo
            surface = Color(0xFFFFFFFF),
            error = Color(0xFFE53935),
            onPrimary = Color.Black,
            onBackground = Color.Black,
            onSurface = Color.Black
        )
    ) {
        CroniotDashboardContent()
    }
}

@Preview(showBackground = true)
@Composable
fun CroniotDashboardPreview5() {
    val gradientBackground = Brush.linearGradient(
        listOf(Color(0xFF1A237E), Color(0xFF4A148C))
    )

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF40C4FF),    // azul eléctrico
            secondary = Color(0xFF7C4DFF),  // púrpura
            background = Color.Transparent,
            surface = Color(0x80212121),    // semitransparente para glass effect
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
        ) {
            CroniotDashboardContent()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CroniotDashboardPreview6() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFFA5D6A7),      // verde menta
            secondary = Color(0xFF81D4FA),    // azul pastel
            background = Color(0xFFFAFAFA),   // gris claro
            surface = Color(0xFFFFFFFF),      // blanco
            error = Color(0xFFE57373),        // rojo pastel
            onPrimary = Color.Black,
            onBackground = Color.Black,
            onSurface = Color.Black
        )
    ) {
        CroniotDashboardContent()
    }
}


@Preview(showBackground = true)
@Composable
fun CroniotDashboardPreview7() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF4081),      // rosa neón
            secondary = Color(0xFF18FFFF),    // cyan neón
            background = Color(0xFF0A0A0F),   // negro azulado
            surface = Color(0xFF212121),      // gris carbón
            error = Color(0xFFFF1744),        // rojo neón
            onPrimary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        CroniotDashboardContent()
    }
}


@Preview(showBackground = true)
@Composable
fun CroniotDashboardPreview8() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF6F00),      // naranja quemado
            secondary = Color(0xFFFFD54F),    // amarillo oro suave
            background = Color(0xFF121212),   // negro grisáceo
            surface = Color(0xFF1E1E1E),      // gris oscuro
            error = Color(0xFFE53935),
            onPrimary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        CroniotDashboardContent()
    }
}
