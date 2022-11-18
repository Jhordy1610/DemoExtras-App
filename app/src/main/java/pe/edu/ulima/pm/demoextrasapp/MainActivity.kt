package pe.edu.ulima.pm.demoextrasapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pe.edu.ulima.pm.demoextrasapp.presentation.MainScreen
import pe.edu.ulima.pm.demoextrasapp.ui.theme.DemoExtrasAppTheme


val CHANNEL_ID = "1"

class MainActivity : ComponentActivity() {

    private var channel: NotificationChannel? = null
    //private lateinit var channel : NotificationChannel //lateinit: indica que channel será inicializado en algún momento


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        setContent {
            MainScreen(
                onNotificationClick = sendNotification
            )
        }
    }

    //Creamos la notificación
    fun createNotification(): Notification {

        val intent = Intent(
            this,
            DestinoActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or //flags: define comportamientos del activity destino
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


}






















