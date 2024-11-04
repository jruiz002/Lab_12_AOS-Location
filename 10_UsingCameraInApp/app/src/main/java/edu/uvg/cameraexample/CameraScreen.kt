package edu.uvg.cameraexample

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

data class ImageWithLocation(val imageBitmap: Bitmap, val latitude: String, val longitude: String)

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageList = remember { mutableStateListOf<ImageWithLocation>() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Launcher para la cámara
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                // Obtener ubicación
                getLocation(fusedLocationClient, context) { latitude, longitude ->
                    imageList.add(ImageWithLocation(it, latitude, longitude))
                }
            }
        }
    }

    // Launcher para solicitar permiso de cámara
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Si el permiso fue otorgado, abre la cámara
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        } else {
            // Maneja la negación del permiso (opcional)
        }
    }

    // Launcher para solicitar permiso de ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permiso de ubicación otorgado
            // Aquí puedes abrir la cámara después de haber obtenido el permiso
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        } else {
            // Maneja la negación del permiso (opcional)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = {
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                PackageManager.PERMISSION_GRANTED -> {
                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        PackageManager.PERMISSION_GRANTED -> {
                            // Ambos permisos otorgados, abre la cámara
                            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            cameraLauncher.launch(cameraIntent)
                        }
                        else -> {
                            // Solicita el permiso de ubicación
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                }
                else -> {
                    // Solicita el permiso de cámara
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }) {
            Text("Capturar Imagen")
        }

        // Grid para mostrar las imágenes capturadas
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(imageList.size) { index ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = imageList[index].imageBitmap.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                    )
                    Text(text = "Lat: ${imageList[index].latitude}, Lon: ${imageList[index].longitude}")
                }
            }
        }
    }
}


private fun getLocation(
    fusedLocationClient: FusedLocationProviderClient,
    context: Context, // Agregamos el contexto aquí
    onLocationResult: (String, String) -> Unit
) {
    // Verificar si el permiso de ubicación está otorgado
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationResult(location.latitude.toString(), location.longitude.toString())
            } else {
                onLocationResult("No location found", "No location found")
            }
        }.addOnFailureListener {
            onLocationResult("Error obtaining location", "Error obtaining location")
        }
    } else {
        // Aquí puedes manejar el caso en que el permiso no está otorgado
        onLocationResult("Permission Denied", "Permission Denied")
    }
}
