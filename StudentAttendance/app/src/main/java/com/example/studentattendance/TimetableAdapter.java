
package com.example.studentattendance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimetableAdapter
        extends RecyclerView.Adapter<TimetableAdapter.TimetableViewHolder> {

    private final List<TimetableItem> timetableList;

    public TimetableAdapter(List<TimetableItem> timetableList) {
        this.timetableList = new ArrayList<>(timetableList);
    }

    @NonNull
    @Override
    public TimetableViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timetable, parent, false);

        return new TimetableViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TimetableViewHolder holder, int position) {

        TimetableItem item = timetableList.get(position);

        holder.txtPeriodNumber.setText(
                String.format(Locale.getDefault(),
                        "Period %d", position + 1));

        holder.txtSubjectName.setText(
                safeText(item.getSubjectName(), "Subject not available"));

        holder.txtPeriodTime.setText(
                formatTime(item.getStartTime()) + " - "
                        + formatTime(item.getEndTime()));

        holder.txtTeacherName.setText(
                "Teacher: "
                        + safeText(item.getTeacherName(), "Not assigned"));
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }

    private String formatTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            return "Time unavailable";
        }

        String value = time.trim();

        String[] inputFormats = {
                "HH:mm:ss",
                "HH:mm",
                "hh:mm a"
        };

        for (String inputFormat : inputFormats) {
            SimpleDateFormat inputFormatter =
                    new SimpleDateFormat(inputFormat, Locale.US);

            inputFormatter.setLenient(false);

            try {
                Date date = inputFormatter.parse(value);

                if (date != null) {
                    SimpleDateFormat outputFormatter =
                            new SimpleDateFormat("hh:mm a", Locale.US);

                    return outputFormatter.format(date);
                }
            } catch (ParseException ignored) {
                // Try the next supported format.
            }
        }

        // Preserve the original value if it has an unexpected format.
        return value;
    }

    @Override
    public int getItemCount() {
        return timetableList.size();
    }

    public void updateData(List<TimetableItem> newList) {
        timetableList.clear();

        if (newList != null) {
            timetableList.addAll(newList);
        }

        notifyDataSetChanged();
    }

    static class TimetableViewHolder extends RecyclerView.ViewHolder {

        TextView txtPeriodNumber;
        TextView txtSubjectName;
        TextView txtPeriodTime;
        TextView txtTeacherName;

        public TimetableViewHolder(@NonNull View itemView) {
            super(itemView);

            txtPeriodNumber =
                    itemView.findViewById(R.id.txtPeriodNumber);

            txtSubjectName =
                    itemView.findViewById(R.id.txtSubjectName);

            txtPeriodTime =
                    itemView.findViewById(R.id.txtPeriodTime);

            txtTeacherName =
                    itemView.findViewById(R.id.txtTeacherName);
        }
    }
}
