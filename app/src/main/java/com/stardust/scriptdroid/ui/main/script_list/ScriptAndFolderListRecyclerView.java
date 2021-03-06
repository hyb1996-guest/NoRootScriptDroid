package com.stardust.scriptdroid.ui.main.script_list;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.workground.WrapContentLinearLayoutManager;

import com.stardust.autojs.script.FileScriptSource;
import com.stardust.scriptdroid.autojs.AutoJs;
import com.stardust.scriptdroid.scripts.ScriptFile;
import com.stardust.scriptdroid.R;
import com.stardust.scriptdroid.scripts.StorageScriptProvider;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import org.greenrobot.eventbus.Subscribe;

/**
 * Created by Stardust on 2017/3/27.
 */

public class ScriptAndFolderListRecyclerView extends RecyclerView {

    public interface OnScriptFileClickListener {

        void onClick(ScriptFile file);
    }

    public interface OnScriptFileLongClickListener {

        void onLongClick(ScriptFile file);
    }

    public interface FileProcessListener {

        void onFilesListing();

        void onFileListed();
    }

    private OnScriptFileClickListener mOnItemClickListener;
    private OnScriptFileLongClickListener mOnItemLongClickListener;
    private final OnClickListener mOnItemClickListenerProxy = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = getChildViewHolder(v).getAdapterPosition();
            if (mCanGoBack && position == 0) {
                goBack();
                return;
            }
            ScriptFile file = mScriptFileList[getActualPosition(position)];
            if (file.isDirectory()) {
                setCurrentDirectory(file, true);
            } else if (mOnItemClickListener != null) {
                mOnItemClickListener.onClick(file);
            }
        }
    };
    private final OnLongClickListener mOnItemLongClickListenerProxy = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (mOnItemLongClickListener != null) {
                int position = getChildViewHolder(v).getAdapterPosition();
                mOnItemLongClickListener.onLongClick(mScriptFileList[getActualPosition(position)]);
                return true;
            }
            return false;
        }
    };

    private OnClickListener mOnRunClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = getChildViewHolder((View) v.getParent()).getAdapterPosition();
            ScriptFile file = mScriptFileList[getActualPosition(position)];
            AutoJs.getInstance().getScriptEngineService().execute(new FileScriptSource(file));
        }
    };

    private ScriptFile[] mScriptFileList = new ScriptFile[0];
    private ScriptFile mCurrentDirectory;
    private ScriptFile mRootDirectory;
    private Adapter mAdapter;
    private boolean mCanGoBack = false;
    private StorageScriptProvider mStorageScriptProvider;
    private FileProcessListener mFileProcessListener;
    private boolean mScriptFileOperationEnabled = true;

    public ScriptAndFolderListRecyclerView(Context context) {
        super(context);
        init();
    }

    public ScriptAndFolderListRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScriptAndFolderListRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void setCurrentDirectory(final ScriptFile folder, boolean canGoBack) {
        mCurrentDirectory = folder;
        mCanGoBack = canGoBack;
        if (mFileProcessListener != null) {
            mFileProcessListener.onFilesListing();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                mScriptFileList = mStorageScriptProvider.getDirectoryScriptFiles(folder);
                post(new Runnable() {
                    @Override
                    public void run() {
                        mFileProcessListener.onFileListed();
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    private void setRootDirectory(ScriptFile folder) {
        mRootDirectory = folder;
        setCurrentDirectory(mRootDirectory, false);
    }

    public void setFileProcessListener(FileProcessListener fileProcessListener) {
        mFileProcessListener = fileProcessListener;
    }

    public void setStorageScriptProvider(StorageScriptProvider storageScriptProvider) {
        if (mStorageScriptProvider != null)
            mStorageScriptProvider.unregisterDirectoryChangeListener(this);
        mStorageScriptProvider = storageScriptProvider;
        mStorageScriptProvider.registerDirectoryChangeListener(this);
        setRootDirectory(mStorageScriptProvider.getInitialDirectory());
    }

    public void setOnItemClickListener(OnScriptFileClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(OnScriptFileLongClickListener onItemLongClickListener) {
        mOnItemLongClickListener = onItemLongClickListener;
    }

    public ScriptFile getCurrentDirectory() {
        return mCurrentDirectory;
    }

    public void setScriptFileOperationEnabled(boolean scriptFileOperationEnabled) {
        mScriptFileOperationEnabled = scriptFileOperationEnabled;
    }

    private void goBack() {
        ScriptFile parent = mCurrentDirectory.getParentFile();
        setCurrentDirectory(parent, !parent.equals(mRootDirectory));
    }

    private void init() {
        setLayoutManager(new WrapContentLinearLayoutManager(getContext()));
        addItemDecoration(new HorizontalDividerItemDecoration.Builder(getContext())
                .color(0xffd9d9d9)
                .size(2)
                .marginResId(R.dimen.script_and_folder_list_divider_left_margin, R.dimen.script_and_folder_list_divider_right_margin)
                .showLastDivider()
                .build());
        mAdapter = new Adapter();
        setAdapter(mAdapter);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superData", super.onSaveInstanceState());
        bundle.putSerializable("current", mCurrentDirectory);
        bundle.putSerializable("root", mRootDirectory);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        mRootDirectory = (ScriptFile) bundle.getSerializable("root");
        mCurrentDirectory = (ScriptFile) bundle.getSerializable("current");
        setCurrentDirectory(mCurrentDirectory, !mCurrentDirectory.equals(mRootDirectory));
        super.onRestoreInstanceState(bundle.getParcelable("superData"));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mStorageScriptProvider != null)
            mStorageScriptProvider.registerDirectoryChangeListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mStorageScriptProvider.unregisterDirectoryChangeListener(this);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (mCanGoBack) {
                goBack();
                return true;
            }
        }
        return false;
    }

    @Subscribe
    public void onDirectoryChange(StorageScriptProvider.DirectoryChangeEvent event) {
        if (event.directory.equals(mCurrentDirectory)) {
            updateCurrentDirectory();
        }
    }

    private void updateCurrentDirectory() {
        setCurrentDirectory(mCurrentDirectory, mCanGoBack);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private final int VIEW_TYPE_FOLDER = 1;
        private final int VIEW_TYPE_FILE = 2;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_FILE:
                    return new FileViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.script_and_folder_list_recycler_view_file, parent, false));
                case VIEW_TYPE_FOLDER:
                    return new ViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.script_and_folder_list_recycler_view_directory, parent, false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (mCanGoBack && position == 0) {
                holder.name.setText("..");
            } else
                holder.bind(mScriptFileList[getActualPosition(position)]);
        }

        @Override
        public int getItemCount() {
            return mScriptFileList.length + (mCanGoBack ? 1 : 0);
        }

        @Override
        public int getItemViewType(int position) {
            if (mCanGoBack && position == 0) {
                return VIEW_TYPE_FOLDER;
            }
            return mScriptFileList[getActualPosition(position)].isDirectory() ? VIEW_TYPE_FOLDER : VIEW_TYPE_FILE;
        }
    }

    private int getActualPosition(int position) {
        return mCanGoBack ? position - 1 : position;
    }


    private class ViewHolder extends RecyclerView.ViewHolder {

        TextView name;

        ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(mOnItemClickListenerProxy);
            itemView.setOnLongClickListener(mOnItemLongClickListenerProxy);
            name = (TextView) itemView.findViewById(R.id.name);
            if (!mScriptFileOperationEnabled) {
                setMarginRight(name, 0);
            }
        }

        private void setMarginRight(View view, int marginRight) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
            layoutParams.rightMargin = marginRight;
            view.setLayoutParams(layoutParams);
        }

        public void bind(ScriptFile file) {
            if (file.isDirectory()) {
                name.setText(file.getName());
            } else {
                name.setText(file.getSimplifiedName());
            }
        }
    }

    private class FileViewHolder extends ViewHolder {

        FileViewHolder(View itemView) {
            super(itemView);
            if (mScriptFileOperationEnabled) {
                itemView.findViewById(R.id.run).setOnClickListener(mOnRunClickListener);
            } else {
                itemView.findViewById(R.id.run).setVisibility(GONE);
            }
        }
    }

}
