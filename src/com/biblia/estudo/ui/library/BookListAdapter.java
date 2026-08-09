package com.biblia.estudo.ui.library;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.biblia.estudo.R;
import com.biblia.estudo.data.FavoriteDao;
import com.biblia.estudo.data.NoteDao;
import com.biblia.estudo.model.Book;

import java.util.List;

public class BookListAdapter extends BaseAdapter {

    private Context context;
    private List<Book> books;
    private LayoutInflater inflater;
    private FavoriteDao favoriteDao;
    private NoteDao noteDao;

    public BookListAdapter(Context context, List<Book> books, FavoriteDao favoriteDao, NoteDao noteDao) {
        this.context = context;
        this.books = books;
        this.favoriteDao = favoriteDao;
        this.noteDao = noteDao;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() { return books.size(); }

    @Override
    public Book getItem(int position) { return books.get(position); }

    @Override
    public long getItemId(int position) { return books.get(position).getId(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_book, parent, false);
            holder = new ViewHolder();
            holder.bookNumber = convertView.findViewById(R.id.bookNumber);
            holder.bookName = convertView.findViewById(R.id.bookName);
            holder.chapterCount = convertView.findViewById(R.id.chapterCount);
            holder.favoriteCount = convertView.findViewById(R.id.favoriteCount);
            holder.notesCount = convertView.findViewById(R.id.notesCount);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Book book = books.get(position);
        holder.bookNumber.setText(String.valueOf(position + 1));
        holder.bookName.setText(book.getName());
        holder.chapterCount.setText(book.getChapterCount() + " cap.");
        holder.chapterCount.setVisibility(View.VISIBLE);

        // Load favorite count
        int favCount = favoriteDao.getCountByBook(book.getId());
        holder.favoriteCount.setText(favCount > 0 ? "★ " + favCount : "");
        holder.favoriteCount.setVisibility(favCount > 0 ? View.VISIBLE : View.GONE);

        // Load notes count
        int notesCount = noteDao.getCountByBook(book.getId());
        holder.notesCount.setText(notesCount > 0 ? "📝 " + notesCount : "");
        holder.notesCount.setVisibility(notesCount > 0 ? View.VISIBLE : View.GONE);

        return convertView;
    }

    static class ViewHolder {
        TextView bookNumber;
        TextView bookName;
        TextView chapterCount;
        TextView favoriteCount;
        TextView notesCount;
    }
}
