package com.alebr.muzic;

public class ArtistItem {

    private String idString;
    private long id;
    private String name;

    public ArtistItem(long id, String name){
        this.id = id;
        this.name = name;

        idString = String.format("artist_%d", id);
    }

    public String getIdString() {
        return idString;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
