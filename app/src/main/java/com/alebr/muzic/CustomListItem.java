package com.alebr.muzic;

class CustomListItem {

    private String id;
    private String title;
    private int imageRes;

    public CustomListItem(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public void changeImage(int imageRes){
        this.imageRes = imageRes;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getImageRes(){return imageRes;}
}
