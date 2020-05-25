package com.example.antonio.lectorclavesnfc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.nfc.NfcManager;
import android.os.Bundle;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
    NfcAdapter adapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag= null;
    Context context;
    String contenidoTag=" "; //Buffer para almacenar el contenido del tag.
    String myButton="SALIR";
    Boolean nfcActivo=true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Definimos el layout a usar
        setContentView(R.layout.activity_main);
        context = this;

        //Elementos que vamos a usar en el layout 2 Botones y Textos.
        Button btnWrite = (Button) findViewById(R.id.button);
        final TextView message = (TextView) findViewById(R.id.edit_message);

        //Botón que muestra el contenido del último Tag "tageado"
        Button btnRead = (Button) findViewById(R.id.button2);
        final TextView readTag = (TextView) findViewById(R.id.ContenidoTag);

       //Botón que borra el contenido de la pantalla y el buffer contenidoTag;
        Button btnErase = (Button)findViewById(R.id.button3);

        checkNFC(); //Verificamos si está activado NFC

        adapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[]{tagDetected};



        /**
         * Cuando pulsemos el Botón escribir Texto (btnWrite)
         * En primer lugar comprobamos Si hay TAG, en Caso afirmativo escribimos el TAG...
         * Mostraremos en pantalla un mensaje tipo Toast informando de si se ha realizado corretamente la escritura.
         */
        btnWrite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //Si no existe tag al que escribir, mostramos un mensaje de NO HAY TAG.
                    if (myTag == null) {
                        Toast.makeText(getApplicationContext(), "ERROR NO HAY TAG", Toast.LENGTH_SHORT).show();
                    } else {
                        //Llamamos al método write que definimos más adelante donde le pasamos por
                        //parámetro el tag que hemos detectado y el mensaje a escribir.
                        write(message.getText().toString(), myTag);
                        //Mostramos por pantalla mediante un toast que todo ha ido bien
                        Toast.makeText(getApplicationContext(), "GRABACION CORRECTA", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "ERROR DE ESCRITURA", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(context, context.getString(R.string.app_name), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        /**
         * Cuando pulsemos el Botón Leer Texto (btnRead)
         * En primer lugar comprobamos Si se ha pasado un tag, en Caso afirmativo escribimos contenidoTag en pantalla
         * Mostraremos en pantalla un mensaje tipo Toast informando de si se ha realizado corretamente la lectura en pantalla.
         */
        btnRead.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                    //Si no existe tag al que escribir, mostramos un mensaje de NO HAY TAG.
                    if (myTag == null) {
                        Toast.makeText(getApplicationContext(), "ERROR NO HAY TAG - TAGEE UNA ETIQUETA PREVIAMENTE", Toast.LENGTH_SHORT).show();
                    } else {
                       readTag.setText(contenidoTag);
                       Toast.makeText(getApplicationContext(), "LECTURA DEL ULTIMO TAG -> OK", Toast.LENGTH_SHORT).show();
                    }
            }
        });

        /**
         * Cuando pulsemos el Botón Borrar (btnRead)
         * Limpiaremos la pantalla, y el contenido del buffer contenidoTag.
         */
        btnErase.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                borrarBuffer(); //llamamos a la función para vaciar el buffer contenido_Tag

                    readTag.setText("Contenido del Tag");
                    message.setText("");
                Toast.makeText(getApplicationContext(), "BORRADO DE BUFFER Y PANTALLA -> OK", Toast.LENGTH_SHORT).show();
                }

        });




    }//onCreate

    /**
     * El método write es el más importante, será el que se encargue de crear el mensaje y escribirlo en el tag.
     *
     * @param text : message capturado del Textview edit_message
     * @param tag  : myTag
     * @throws IOException     : Por defecto errores en la i/o
     * @throws FormatException : Errores en el formato
     */
    private void write(String text, Tag tag) throws IOException, FormatException {
        //Creamos un array de elementos NdefRecord. Este Objeto representa un registro del mensaje NDEF
        //Para crear el objeto NdefRecord usamos el método createRecord(String s)
        NdefRecord[] records = {createRecord(text)};

        //NdefMessage encapsula un mensaje Ndef(NFC Data Exchange Format). Estos mensajes están
        //compuestos por varios registros encapsulados por la clase NdefRecord
        NdefMessage message = new NdefMessage(records);

        //Obtenemos una instancia de Ndef del Tag
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    /**
     * Método createRecord será el que nos codifique el mensaje para crear un NdefRecord usando RTD_TEXT
     * @param text : le pasamos text desde write que es message.getText().toString() , el contenido del mensaje texto introducido.
     * @return recordNFC un objeto NdefRecord con el formato correcto para Texto
     * @throws UnsupportedEncodingException : Por si le pasamos un Formato no soportado (Signos no soportados por US-ASCII).
     */
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException{
        String lang = "us";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payLoad = new byte[1 + langLength + textLength];

        payLoad[0] = (byte) langLength;

        System.arraycopy(langBytes, 0, payLoad, 1, langLength);
        System.arraycopy(textBytes, 0, payLoad, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payLoad);

        return recordNFC;
    }

    /**
     * Borra el contenido del buffer temporal contenidoTag e inicializa myTag a null
     */
    public void borrarBuffer(){
        this.contenidoTag="";
        this.myTag=null;
    }
    /**
     * onnewIntent manejamos el intent para encontrar el Tag pasado por nuestra antena NFC
     * @param intent
     */
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG); //Usamos EXTRA_TAG para conocer las tecnologías de nuestra etiqueta.
        NdefMessage[] messages = null;
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        String aux="";

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {

            //Comprobamos si la etiqueta está vacia. o no está escrita en NDEF como por ejemplo una tarjeta bancaria
            if (rawMsgs == null) {
                myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                Toast.makeText(getApplicationContext(), "ETIQUETA VACIA O NO NDEF", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(),"TECNOLOGIAS DE LA ETIQUETA:", Toast.LENGTH_SHORT).show();
                Toast.makeText(this,  myTag.toString(), Toast.LENGTH_LONG).show();
            }

            //Si la etiqueta no está vacia
            if (rawMsgs != null) {
                messages = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    messages[i] = (NdefMessage) rawMsgs[i];
                }

                if (messages[0] != null) {
                    String result = "";
                    byte[] payload = messages[0].getRecords()[0].getPayload();

                    //Asumimos que va a devolver la sección escrita del tag leido

                    //Comenzamos a leer desde la 3 posición para que no mostrar en el toast el código de País.
                    for (int b = 3; b < payload.length; b++) {
                        result += (char) payload[b];
                    }
                    this.contenidoTag=result; //mandamos el contenido de la etiqueta a la variable contenidotag.

                    //mostramos mediante mensajes toast los resultados de la lectura
                    Toast.makeText(getApplicationContext(), "DATOS LEIDOS:  " + result, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }//onNewIntent


    void checkNFC(){
        NfcManager manager = (NfcManager) this.getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        if (adapter != null && adapter.isEnabled()) {
            //NFC ACTIVO sigue.

        }
        else{
            this.nfcActivo=false;
            this.myButton="SALIR";
            myDialog("Active su NFC y vuelva a lanzar la aplicación, o este dispositivo no tiene NFC ",this);

        }
    }


    /**
     * Muestra mensaje de alerta en pantalla.
     * @param myWarning , es el mensaje de error que mostrará por pantalla
     */
    public void myDialog(String myWarning, Activity myActivity){
        final AlertDialog.Builder builder = new AlertDialog.Builder(myActivity);

        builder.setTitle("ATENCIÓN");
        builder.setMessage(myWarning);
        builder.setPositiveButton(myButton, new DialogInterface.OnClickListener() {



            public void onClick (DialogInterface dialog,int which)
            {
                if(myButton.equals("SALIR"))
                {
                    //Caso para salir de la aplicación
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(1);}
                else{
                    //No hace nada, es para botón aceptar, sigue la ejecución normal
                }
            }

        });

        final AlertDialog dialog = builder.create();
        dialog.show(); //show() should be called before dialog.getButton().
    }


 //Definimos los estados:

    public void onPause(){
        super.onPause();
        if(nfcActivo){
            WriteModeOff();
        }
    }
    public void onResume(){
        super.onResume();
        if(nfcActivo) {
            WriteModeOn();
        }
    }

    private void WriteModeOn(){
        writeMode = true;
        adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff(){
        writeMode = false;
        adapter.disableForegroundDispatch(this);
    }

}//Main

