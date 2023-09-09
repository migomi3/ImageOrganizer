package com.example.imageorganizer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

public class GalleryAdaptor extends RecyclerView.Adapter<GalleryAdaptor.ViewHolder> {

    private final Context context;
    private final ArrayList<String> image_list;

    public GalleryAdaptor(Context context, ArrayList<String> image_list) {
        this.context = context;
        this.image_list = image_list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File img_file = new File(image_list.get(position));
        Picasso.get().setLoggingEnabled(true);

        if(img_file.exists()) {
            Picasso.get().load(img_file).placeholder(R.drawable.ic_launcher_foreground).into(holder.image);

            holder.itemView.setOnClickListener(view -> {
                Intent i = new Intent(context, ImageDetail.class);
                i.putExtra("imgPath", image_list.get(holder.getAdapterPosition()));
                context.startActivity(i);
            });
        }
    }

    @Override
    public int getItemCount() {
        return this.image_list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        private final ImageView image;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.gallery_item);
        }
    }
}
