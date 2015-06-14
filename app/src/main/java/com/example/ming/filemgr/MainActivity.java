package com.example.ming.filemgr;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {

    public static final String SET_ROOT_PATH = "set_root_path";

    public static final String OPEN_MODE = "open_mode";
    public static final int OPEN_FILE = 101;
    @SuppressWarnings("unused")
    public static final int CHOOSE_FILE = 102;

    public static final int FILE_SELECTED = 110;
    public static final String SELECTED_FILE = "selected_file";

    private static final int FILE_SELECT_CODE = 0;
    private static final String SHOW_HIDDEN = "show_hidden";

    int mMenuID = R.menu.menu_main;

    ListView mListView;
    File[] mFileList = new File[0];
    FilesAdapter mFilesAdapter;
    File mCurrentDirectory;
    String mRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    boolean mShowHidden;

    boolean mIsCopying;
    boolean mIsCutting;
    File mFromFile;
    File mToFile;
    int mSelectedIndex = -1;
    private boolean mIsOpenFile;

    private void setRootPath(String s) throws FileNotFoundException {
        File f = new File(s);
        if (!f.exists()) {
            throw new FileNotFoundException(s + "is not found.");
        }

        mRootPath = s;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mShowHidden = savedInstanceState.getBoolean(SHOW_HIDDEN, false);
        }

        String rootPath = getIntent().getStringExtra(SET_ROOT_PATH);
        if (rootPath != null) {
            try {
                setRootPath(rootPath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        mIsOpenFile = getIntent().getIntExtra(OPEN_MODE, 101) == OPEN_FILE;

        mListView = (ListView) findViewById(R.id.listView);

        mCurrentDirectory = Environment.getExternalStorageDirectory();
        mFilesAdapter = new FilesAdapter(this, this, android.R.layout.simple_list_item_1, mFileList);

        mListView.setAdapter(mFilesAdapter);
        refreshFileList();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = mFileList[position];

                if (file.isDirectory()) {
                    mCurrentDirectory = file;
                    refreshFileList();
                } else {
                    Toast.makeText(MainActivity.this, file.getName(), Toast.LENGTH_SHORT).show();

                    onFileSelected(file.getAbsolutePath());
                }
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                makeSelection(position);

                mMenuID = R.menu.menu_choosed;
                invalidateOptionsMenu();

                return true;
            }
        });
    }

    private void onFileSelected(String path) {
        Intent intent = new Intent();

        if (mIsOpenFile) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);
            String type = FileUtil.getMIME(path.substring(path.lastIndexOf(".") + 1, path.length()));
            intent.setDataAndType(Uri.parse(path), type);
            try {
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Please install app for opening file with " + type, Toast.LENGTH_SHORT).show();
            }
        } else {
            intent.putExtra(SELECTED_FILE, path);
            setResult(FILE_SELECTED, intent);
            finish();
        }
    }

    private void makeSelection(int position) {
        mSelectedIndex = position;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFilesAdapter.notifyDataSetChanged();
            }
        });
    }

    private void refreshFileList() {
        mFileList = getFiles(mCurrentDirectory);
        onFileArrayChanged();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SHOW_HIDDEN, mShowHidden);
    }

    private File[] getFiles(File file) {
        return file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.isHidden() || mShowHidden;
            }
        });
    }

    private void onFileArrayChanged() {
        sortFileList();
        setTitle();

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFilesAdapter.setFiles(mFileList);
                mFilesAdapter.notifyDataSetChanged();
            }
        });
    }

    private void sortFileList() {
        Arrays.sort(mFileList, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });
    }

    private void setTitle() {
        String title;

        if (mCurrentDirectory.getAbsolutePath().equals(mRootPath)) {
            title = "File Chooser";
        } else {
            title = mCurrentDirectory.getName();
        }

        setTitle(title);
    }

    @Override
    public void onBackPressed() {
        if (mMenuID == R.menu.menu_choosed) {
            onOperationDone();
            exitSelection();
            return;
        }

        if (!mCurrentDirectory.getAbsolutePath()
                .equals(mRootPath)) {
            mCurrentDirectory = mCurrentDirectory.getParentFile();
            refreshFileList();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(mMenuID, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = true;

        switch (item.getItemId()) {
            case R.id.action_other_filemgr:
                FileUtil.showFileChooser("", this, FILE_SELECT_CODE);
                break;
            case R.id.action_toggle_hide:
                toggleShowHidden(item);
                break;
            case R.id.action_copy:
                mFromFile = (File) mFilesAdapter.getItem(mSelectedIndex);
                mIsCopying = true;
                exitSelection();
                break;
            case R.id.action_cut:
                mFromFile = (File) mFilesAdapter.getItem(mSelectedIndex);
                mIsCutting = true;
                exitSelection();
                break;
            case R.id.action_paste:
                mToFile = mCurrentDirectory;
                if (mIsCopying) {
                    File file = copyTo(mFromFile, mToFile);
                    if (file != null) {
                        notifyOperationDone(file, "copied");
                    }
                } else if (mIsCutting) {
                    File file = moveTo(mFromFile, mToFile);
                    if (file != null) {
                        notifyOperationDone(file, "moved");
                    }
                } else {
                    Toast.makeText(this, "Please selected one file first.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_delete:
                mFromFile = (File) mFilesAdapter.getItem(mSelectedIndex);
                if (delete(mFromFile)) {
                    notifyOperationDone(mFromFile, "deleted");
                    exitSelection();
                }
                break;
            case R.id.action_new_file:
                final EditText txtFile = new EditText(this);
                new AlertDialog.Builder(this)
                        .setTitle("Please enter the new file's name:")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setView(txtFile)
                        .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String name = String.valueOf(txtFile.getText());
                                File destFile = new File(mCurrentDirectory, name);
                                try {
                                    if (destFile.createNewFile()) {
                                        refreshFileList();
                                    } else {
                                        Toast.makeText(MainActivity.this,
                                                "File " + name + " already exist.", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null).show();
                break;
            case R.id.action_new_directory:
                final EditText txtDir = new EditText(this);
                new AlertDialog.Builder(this)
                        .setTitle("Please enter the new directory's name:")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setView(txtDir)
                        .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String name = String.valueOf(txtDir.getText());
                                File destFile = new File(mCurrentDirectory, name);
                                if (destFile.mkdir()) {
                                    refreshFileList();
                                } else {
                                    Toast.makeText(MainActivity.this,
                                            "Directory " + name + " already exist.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null).show();
                break;
            default:
                handled = super.onOptionsItemSelected(item);
        }
        return handled;
    }

    private boolean delete(File file) {
        boolean hasDeleted = true;

        if (file.isFile()) {
            hasDeleted = file.delete();
        } else {
            File[] files = file.listFiles();
            for (File f : files) {
                hasDeleted = delete(f);
            }
            hasDeleted = hasDeleted && file.delete();
        }

        return hasDeleted;
    }

    private void onOperationDone() {
        mFromFile = null;
        mToFile = null;
        mIsCopying = false;
        mIsCutting = false;
    }

    private void exitSelection() {
        mMenuID = R.menu.menu_main;
        invalidateOptionsMenu();

        mSelectedIndex = -1;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFilesAdapter.notifyDataSetChanged();
            }
        });
    }

    private File copyTo(File fromFile, File toFile) {
        FileInputStream fis;
        FileOutputStream fos;
        FileChannel from;
        FileChannel to;

        File destFile = new File(toFile.getAbsolutePath() + "/" + fromFile.getName());
        try {
            if (fromFile.isDirectory()) {
                if (!destFile.mkdir()) {
                    Toast.makeText(this, "Directory " + destFile + " exist.", Toast.LENGTH_SHORT).show();
                    destFile = null;
                }
            } else {
                if (!destFile.createNewFile()) {
                    Toast.makeText(this, "File " + destFile + " exist.", Toast.LENGTH_SHORT).show();
                    destFile = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            destFile = null;
        }

        if (destFile != null) {
            if (fromFile.isFile()) {
                try {
                    fis = new FileInputStream(fromFile);
                    fos = new FileOutputStream(destFile);
                    from = fis.getChannel();
                    to = fos.getChannel();

                    from.transferTo(0, from.size(), to);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    destFile = null;
                }
            } else {
                File[] files = fromFile.listFiles();
                for (File f : files) {
                    copyTo(f, destFile);
                }
            }
        }
        return destFile;
    }

    private File moveTo(File fromFile, File toFile) {
        File moved = copyTo(fromFile, toFile);

        if (moved != null && !delete(fromFile)) {
            Toast.makeText(this, "Deleting file" + mFromFile.getName() + "failed.", Toast.LENGTH_SHORT).show();
            moved = null;
        }
        return moved;
    }

    private void notifyOperationDone(File toFile, String operation) {
        Toast.makeText(this,
                toFile.getName() + " has successfully " + operation,
                Toast.LENGTH_SHORT).show();
        onOperationDone();
        refreshFileList();
    }

    private void toggleShowHidden(MenuItem item) {
        if (mShowHidden) {
            item.setTitle(getResources().getString(R.string.action_show_hidden));
            item.setIcon(android.R.drawable.ic_menu_view);
            mShowHidden = false;
            refreshFileList();
            mListView.smoothScrollToPosition(0);
        } else {
            item.setTitle(getResources().getString(R.string.action_hide_hidden));
            item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
            mShowHidden = true;
            refreshFileList();
            mListView.smoothScrollToPosition(0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    String path = FileUtil.getPath(this, data.getData());

                    if (path != null) {
                        Toast.makeText(this, FileUtil.resolveFileName(path), Toast.LENGTH_SHORT).show();
                        onFileSelected(path);
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}
