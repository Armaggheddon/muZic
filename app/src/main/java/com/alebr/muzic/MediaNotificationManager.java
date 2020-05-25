package com.alebr.muzic;

public class MediaNotificationManager {
    /*
    Gestisce l'invio delle notifiche multimediali
    Si occupa di creare un nuovo canale per le notifiche se non già creato
    e permette la modifica delle notifiche pubblicate per rappresentare lo
    stato corrente della riproduzione
     */
    private static final String TAG = "MediaNotificationManager";
    public static final String CHANNEL_ID = "muZic";
    //L'id delle notifiche è un intero diverso da 0
    //Poiche non usiamo più notifiche contemporaneamente non è necessario
    //generare gli ID dinamicamente, anzi, utilizzando lo stesso ID per tutte
    //le notifiche possiamo aggiornare la notifica che è attualmente mostrata
    public static final int NOTIFICATION_ID = 100;
}
