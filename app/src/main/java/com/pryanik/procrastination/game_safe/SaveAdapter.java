package com.pryanik.procrastination.game_safe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pryanik.procrastination.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class SaveAdapter extends ArrayAdapter<SaveItem> {
    private final LayoutInflater inflater;
    private final int layout;
    private final List<SaveItem> states;

    public SaveAdapter(Context context, int resource, List<SaveItem> states) {
        super(context, resource, states);
        this.states = states;
        this.layout = resource;
        this.inflater = LayoutInflater.from(context);
    }

    @NotNull
    public View getView(int position, View convertView, @NotNull ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(this.layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        SaveItem state = states.get(position);
        viewHolder.imageView.setImageBitmap(state.getPicture());
        viewHolder.nameView.setText(state.getStr());
        viewHolder.capitalView.setText(state.getSubStr());
        return convertView;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaveAdapter that = (SaveAdapter) o;
        return layout == that.layout &&
                Objects.equals(inflater, that.inflater) &&
                Objects.equals(states, that.states);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inflater, layout, states);
    }

    @NotNull
    @Override
    public String toString() {
        return "SaveAdapter{" +
                "inflater=" + inflater +
                ", layout=" + layout +
                ", states=" + states +
                '}';
    }

    private static class ViewHolder {
        final ImageView imageView;
        final TextView nameView, capitalView;

        ViewHolder(@NotNull View view) {
            imageView = view.findViewById(R.id.im_picture);
            nameView = view.findViewById(R.id.tv_node_id);
            capitalView = view.findViewById(R.id.tv_date);
        }
    }
}