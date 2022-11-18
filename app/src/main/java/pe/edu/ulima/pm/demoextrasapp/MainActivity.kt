package pe.edu.ulima.pm.demoextrasapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import pe.edu.ulima.pm.demoextrasapp.presentation.MainScreen


val CHANNEL_ID = "1"

class MainActivity : ComponentActivity() {

    private var channel: NotificationChannel? = null
    //private lateinit var channel : NotificationChannel //lateinit: indica que channel será inicializado en algún momento


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestLocationPermission()
        createNotificationChannel()

        setContent {
            MainScreen(
                onNotificationClick = sendNotification,
                onObtenerLocalizacionClick = obtenerLocalizacion
            )
        }
    }

    //Creamos la notificación
    fun createNotification(): Notification {

        val intent = Intent(
            this,
            DestinoActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or //flags: define comportamientos del activity destino
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { //debido a cambios en la API 33
                PendingIntent.getActivity( //intent retrasado, queda pendiente esperando algún evento (hacer click a la notif)
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_MUTABLE //esto es lo nuevo desde la API 33
                )
            } else {
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    0
                )
            }

        val notif =
            NotificationCompat.Builder(this, CHANNEL_ID) //se llama al canal de notif CHANNEL_ID
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Notificación PM")
                .setContentText("Notificación de Prueba!!")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) //sólo hace un sonido, no aparece en el status bar, se debe deslizar para verlo
                .build()

        return notif
    }


    //Ejecutar la notificación (en modo lambda para poder enviarlo como parámetro al MainScreen - Callback)
    val sendNotification: () -> Unit = {
        val notif = createNotification() //lamamos la fun para crear la notif
        val notiManager =
            NotificationManagerCompat.from(this) //notiManager es una referencia a NotificationManager
        notiManager.notify(1, notif) //en el androidmanifest se añade el permiso de notif
    }


    //Creamos el canal de notificación
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            channel = NotificationChannel(
                CHANNEL_ID, //identificador del canal
                "Canal de notificación 1", //nombre del canal
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description =
                    "Es una descripción" //la función apply nos da propiedades extras fuera del constructor actual
            }
            //channel.description = "Mi descripción" //si no usamos apply, se usa esto

            //Registramos el canal de notificación
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager //una notif es un servicio del SO. getSystemService solo funciona a nivel de Activity

            notificationManager.createNotificationChannel(channel!!)
        }

    }

    // Localización

    private val requestPermissionsLauncher =
        registerForActivityResult( // acá ya está predefinida la pantallita para pedir permisos
            ActivityResultContracts.RequestPermission()
        ) { seOtorgaronPermisos ->

            if (seOtorgaronPermisos) {
                Log.i("Location", "Se otorgaron los permisos")
            } else {
                Log.i("Location", "No se otorgaron los permisos")
            }
        }


    // Ventana modal para pedir permisos al usuario, este elige si concede o no los permisos (al usar la App)
    private fun requestLocationPermission() {

        when {
            // 1. El usuario ya dio los permisos antes (para todas las veces)
            ContextCompat.checkSelfPermission( //checkear si el user ya dio permiso
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("Location", "El usuario ya dio permisos")
            }


            // 2. No dio los permisos y se necesitaban
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                finish()
            }

            // 3. Aun no le ha dado los permisos
            else -> {
                // Lanzar pantalla para pedir permisos
                requestPermissionsLauncher.launch(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            }


        }
    }

    // Obtener la última localización
    @SuppressLint("MissingPermission") //para que no sea necesario volver a pedir los permisos
    private val obtenerUltimaLocalizacion: () -> Unit = {

        val fusedLocationClient =
            // fusedLocationCliente es un objeto que conecta con google services para ubicar la localización
            LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        )  //Acá android usa GPS para ubicar gracias al PRIORITY_HIGH_ACCURACY (es más exacto)
            .addOnSuccessListener {
                if (it != null) {
                    Log.i("Location", "Latitud: ${it.latitude} Longitud: ${it.longitude}")
                }
            }

//        fusedLocationClient.lastLocation.addOnSuccessListener {
//            if (it != null) {
//                Log.i("Location", "Latitud: ${it.latitude} Longitud: ${it.longitude}")
//            }
//        }
    }

    //Obtener los updates de Localizacion constantemente
    @SuppressLint("MissingPermission")
    private val obtenerLocalizacion: () -> Unit = {

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        //Para añadir parámetros
        //val locationRequest = LocationRequest.Builder(3000).build() // cada 3 seg (era otra manera)
        val locationRequest = com.google.android.gms.location.LocationRequest().apply {
            priority = Priority.PRIORITY_LOW_POWER
            setInterval(3000)
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() { //LocationCallback() es una interface, por eso se implementan sus métodos
                override fun onLocationResult(location: LocationResult) {
                    super.onLocationResult(location)
                    Log.i(
                        "Location",
                        "Lat: ${location.lastLocation!!.latitude} " +
                                "Long: ${location.lastLocation!!.longitude}"
                    )
                }
            },
            Looper.getMainLooper() //un loop para los updates constantes
        )

    }


}






















