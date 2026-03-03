package com.github.indigopolecat.bingobrewers.config;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.awt.Color;
import java.io.IOException;

public class ColorTypeAdapter extends TypeAdapter<Color> {
    @Override
    public void write(JsonWriter out, Color color) throws IOException {
        if (color == null) {
            out.nullValue();
            return;
        }
        out.value(color.getRGB());
    }

    @Override
    public Color read(JsonReader in) throws IOException {
        return new Color(in.nextInt(), true);
    }
}
