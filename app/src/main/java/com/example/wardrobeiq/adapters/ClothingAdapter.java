package com.example.wardrobeiq.adapters;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;

import com.example.wardrobeiq.activities.ClothingDetailsActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wardrobeiq.R;
import com.example.wardrobeiq.models.Clothing;

import java.util.List;

public class ClothingAdapter
        extends RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder> {

    private List<Clothing> clothingList;

    public ClothingAdapter(List<Clothing> clothingList) {
        this.clothingList = clothingList;
    }

    @NonNull
    @Override
    public ClothingViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_clothing,
                        parent,
                        false
                );

        return new ClothingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ClothingViewHolder holder,
            int position) {

        Clothing clothing = clothingList.get(position);

        holder.txtCategory.setText(
                clothing.getCategory()
        );

        holder.txtColor.setText(
                clothing.getColor()
                        + " • "
                        + clothing.getPattern()
        );

        String imagePath =
                clothing.getImagePath();

        if (imagePath != null
                && !imagePath.isEmpty()) {

            BitmapFactory.Options options =
                    new BitmapFactory.Options();

            options.inSampleSize = 2;

            android.graphics.Bitmap bitmap =
                    BitmapFactory.decodeFile(
                            imagePath,
                            options
                    );

            if (bitmap != null) {

                holder.imgClothing.setImageBitmap(bitmap);

            } else {

                holder.imgClothing.setImageResource(
                        android.R.drawable.ic_menu_gallery
                );
            }

        } else {

            holder.imgClothing.setImageResource(
                    android.R.drawable.ic_menu_gallery
            );
        }

        // OPEN CLOTHING DETAILS
        holder.itemView.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            v.getContext(),
                            ClothingDetailsActivity.class
                    );

            intent.putExtra(
                    "clothingId",
                    clothing.getId()
            );

            v.getContext().startActivity(intent);
        });
    }



    @Override
    public int getItemCount() {
        return clothingList.size();
    }

    public void updateList(List<Clothing> newList) {

        clothingList = newList;

        notifyDataSetChanged();
    }


    static class ClothingViewHolder
            extends RecyclerView.ViewHolder {

        ImageView imgClothing;

        TextView txtCategory;

        TextView txtColor;

        public ClothingViewHolder(
                @NonNull View itemView) {

            super(itemView);

            imgClothing =
                    itemView.findViewById(
                            R.id.imgClothing
                    );

            txtCategory =
                    itemView.findViewById(
                            R.id.txtCategory
                    );

            txtColor =
                    itemView.findViewById(
                            R.id.txtColor
                    );
        }
    }
}