package com.github.indigopolecat.bingobrewers.util;

import java.util.ArrayList;

public class Chest {

    public int x;
    public int y;
    public int z;
    public String id;
    public ArrayList<String> items = new ArrayList<>();
    public ArrayList<Integer> clientConfirmations = new ArrayList<>(); // Clients who contributed or confirmed this data, 5 to lock
    public ArrayList<Integer> clientFakeReports = new ArrayList<>(); // Clients who reported this data as fake, 2 to delete
    public boolean locked = false;

    public Chest(int x, int y, int z, ArrayList<String> items) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.id = x + "" + y + z;
        this.items = items;
    }
}
