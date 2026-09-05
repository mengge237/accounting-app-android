package com.smxy.myapplication.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smxy.myapplication.R;
import com.smxy.myapplication.adapter.NoteAdapter;
import com.smxy.myapplication.model.Note;
import com.smxy.myapplication.network.ApiClient;
import com.smxy.myapplication.network.ApiService;
import com.smxy.myapplication.network.NoteResponse;
import com.smxy.myapplication.utils.ErrorHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;

public class NoteListFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;
    private NoteAdapter adapter;
    private final List<Note> noteList = new ArrayList<>();
    private ApiService apiService;
    private String authToken;
    private SimpleDateFormat dateFormat;

    // 是否使用Mock数据（当后端接口未实现时使用）
    private boolean useMockData = true;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_notes);
        tvEmpty = view.findViewById(R.id.tv_empty_notes);
        fabAdd = view.findViewById(R.id.fab_add_note);

        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!ErrorHandler.isFragmentValid(this)) return;

        setupRecyclerView();
        setupListeners();

        if (useMockData) {
            // 使用Mock数据
            loadMockNotes();
        } else {
            // 使用真实API
            loadToken();
            loadNotes();
        }
    }

    private void setupRecyclerView() {
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NoteAdapter(new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note) {
                if (note != null) showNoteDetail(note);
            }

            @Override
            public void onNoteLongClick(Note note) {
                if (note != null) showDeleteConfirmDialog(note);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> showAddNoteDialog());
        }
    }

    private void loadToken() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        SharedPreferences userPrefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String token = userPrefs.getString("token", "");
        if (token.isEmpty()) {
            SharedPreferences authPrefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
            token = authPrefs.getString("token", "");
        }
        authToken = "Bearer " + token;
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    // Mock数据方法 - 加载示例便签
    private void loadMockNotes() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        // 模拟网络延迟
        mainHandler.postDelayed(() -> {
            if (!ErrorHandler.isFragmentValid(NoteListFragment.this)) return;

            noteList.clear();

            // 添加一些示例便签
            Note note1 = new Note();
            note1.setId(1);
            note1.setTitle("欢迎使用便签功能");
            note1.setContent("这是一个示例便签，你可以添加、编辑和删除便签。\n\n后端API接口还未实现，当前使用的是Mock数据模式。\n\n提示：长按便签可以删除，点击便签可以查看详情并编辑。");
            note1.setCreateTime(new Date());

            Note note2 = new Note();
            note2.setId(2);
            note2.setTitle("待办事项");
            note2.setContent("1. 完成项目报告\n2. 购买食材\n3. 预约医生\n4. 回复邮件\n5. 整理文档");
            note2.setCreateTime(new Date(System.currentTimeMillis() - 86400000)); // 一天前

            Note note3 = new Note();
            note3.setId(3);
            note3.setTitle("购物清单");
            note3.setContent("• 牛奶\n• 面包\n• 鸡蛋\n• 水果\n• 蔬菜\n• 零食");
            note3.setCreateTime(new Date(System.currentTimeMillis() - 172800000)); // 两天前

            Note note4 = new Note();
            note4.setId(4);
            note4.setTitle("工作备忘");
            note4.setContent("周一：团队会议\n周二：客户演示\n周三：代码审查\n周四：文档编写\n周五：项目总结");
            note4.setCreateTime(new Date(System.currentTimeMillis() - 259200000)); // 三天前

            noteList.addAll(java.util.Arrays.asList(note1, note2, note3, note4));

            if (adapter != null) {
                adapter.updateNotes(noteList);
            }

            updateEmptyView();
        }, 500);
    }

    private void updateEmptyView() {
        if (tvEmpty != null) {
            if (noteList.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("暂无便签\n点击 + 添加便签");
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(noteList.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void loadNotes() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        if (!ErrorHandler.isTokenValid(authToken)) {
            if (tvEmpty != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("请先登录");
            }
            return;
        }

        Call<NoteResponse> call = apiService.getNotes(authToken);
        call.enqueue(new ErrorHandler.SafeCallback<NoteResponse>(this) {
            @Override
            protected void onSuccess(NoteResponse data) {
                if (!ErrorHandler.isFragmentValid(NoteListFragment.this)) return;

                noteList.clear();
                if (data.getNotes() != null) {
                    noteList.addAll(data.getNotes());
                }
                if (adapter != null) {
                    adapter.updateNotes(noteList);
                }

                updateEmptyView();
            }

            @Override
            protected void onError(String message) {
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(ErrorHandler.getSafeString(message, "加载失败\n使用Mock数据模式"));
                }
                // 如果API失败，回退到Mock数据
                if (useMockData) {
                    loadMockNotes();
                }
            }
        });
    }

    private void showAddNoteDialog() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_note, null);

        EditText etTitle = view.findViewById(R.id.et_note_title);
        EditText etContent = view.findViewById(R.id.et_note_content);

        builder.setTitle("添加便签")
                .setView(view)
                .setPositiveButton("保存", (dialog, which) -> {
                    String title = etTitle != null ? etTitle.getText().toString().trim() : "";
                    String content = etContent != null ? etContent.getText().toString().trim() : "";

                    if (title.isEmpty()) {
                        ErrorHandler.showToast(requireContext(), "请输入标题");
                        return;
                    }

                    if (useMockData) {
                        addMockNote(title, content);
                    } else {
                        addNote(title, content);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // Mock添加便签
    private void addMockNote(String title, String content) {
        Note newNote = new Note();
        newNote.setId((int) (System.currentTimeMillis() % 100000));
        newNote.setTitle(title);
        newNote.setContent(content);
        newNote.setCreateTime(new Date());

        noteList.add(0, newNote);
        adapter.updateNotes(noteList);
        updateEmptyView();

        ErrorHandler.showToast(getContext(), "便签添加成功");
    }

    private void addNote(String title, String content) {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;

        Map<String, String> noteData = new HashMap<>();
        noteData.put("title", title);
        noteData.put("content", content);

        Call<NoteResponse> call = apiService.addNote(authToken, noteData);
        call.enqueue(new ErrorHandler.SafeCallback<NoteResponse>(this) {
            @Override
            protected void onSuccess(NoteResponse data) {
                ErrorHandler.showToast(getContext(), "便签添加成功");
                loadNotes();
            }

            @Override
            protected void onError(String message) {
                ErrorHandler.showToast(getContext(), ErrorHandler.getSafeString(message, "添加失败"));
            }
        });
    }

    private void showNoteDetail(Note note) {
        if (!ErrorHandler.isFragmentValid(this) || note == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_note_detail, null);

        TextView tvTitle = view.findViewById(R.id.tv_detail_note_title);
        TextView tvContent = view.findViewById(R.id.tv_detail_note_content);
        TextView tvTime = view.findViewById(R.id.tv_detail_note_time);
        Button btnEdit = view.findViewById(R.id.btn_edit_note);
        Button btnDelete = view.findViewById(R.id.btn_delete_note);

        if (tvTitle != null) tvTitle.setText(ErrorHandler.getSafeString(note.getTitle(), "无标题"));
        if (tvContent != null) tvContent.setText(ErrorHandler.getSafeString(note.getContent(), "无内容"));
        if (tvTime != null && note.getCreateTime() != null) {
            String timeText = "创建时间: " + dateFormat.format(note.getCreateTime());
            tvTime.setText(timeText);
        }

        AlertDialog dialog = builder.setView(view).create();

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                dialog.dismiss();
                showEditNoteDialog(note);
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                showDeleteConfirmDialog(note);
            });
        }

        dialog.show();
    }

    private void showEditNoteDialog(Note note) {
        if (!ErrorHandler.isFragmentValid(this) || note == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_note, null);

        EditText etTitle = view.findViewById(R.id.et_note_title);
        EditText etContent = view.findViewById(R.id.et_note_content);

        if (etTitle != null) etTitle.setText(ErrorHandler.getSafeString(note.getTitle(), ""));
        if (etContent != null) etContent.setText(ErrorHandler.getSafeString(note.getContent(), ""));

        builder.setTitle("编辑便签")
                .setView(view)
                .setPositiveButton("保存", (dialog, which) -> {
                    String title = etTitle != null ? etTitle.getText().toString().trim() : "";
                    String content = etContent != null ? etContent.getText().toString().trim() : "";

                    if (title.isEmpty()) {
                        ErrorHandler.showToast(requireContext(), "请输入标题");
                        return;
                    }

                    if (useMockData) {
                        updateMockNote(note, title, content);
                    } else {
                        updateNote(note.getId(), title, content);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // Mock更新便签
    private void updateMockNote(Note note, String title, String content) {
        note.setTitle(title);
        note.setContent(content);
        note.setCreateTime(new Date());

        adapter.updateNotes(noteList);
        ErrorHandler.showToast(getContext(), "更新成功");
    }

    private void updateNote(int noteId, String title, String content) {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;

        Map<String, String> noteData = new HashMap<>();
        noteData.put("title", title);
        noteData.put("content", content);

        Call<NoteResponse> call = apiService.updateNote(authToken, noteId, noteData);
        call.enqueue(new ErrorHandler.SafeCallback<NoteResponse>(this) {
            @Override
            protected void onSuccess(NoteResponse data) {
                ErrorHandler.showToast(getContext(), "更新成功");
                loadNotes();
            }

            @Override
            protected void onError(String message) {
                ErrorHandler.showToast(getContext(), ErrorHandler.getSafeString(message, "更新失败"));
            }
        });
    }

    private void showDeleteConfirmDialog(Note note) {
        if (!ErrorHandler.isFragmentValid(this) || note == null) return;

        String message = "确定要删除便签 \"" + ErrorHandler.getSafeString(note.getTitle(), "这个便签") + "\" 吗？";
        new AlertDialog.Builder(requireContext())
                .setTitle("删除便签")
                .setMessage(message)
                .setPositiveButton("确定", (dialog, which) -> {
                    if (useMockData) {
                        deleteMockNote(note);
                    } else {
                        deleteNote(note.getId());
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // Mock删除便签
    private void deleteMockNote(Note note) {
        noteList.remove(note);
        adapter.updateNotes(noteList);
        updateEmptyView();
        ErrorHandler.showToast(getContext(), "删除成功");
    }

    private void deleteNote(int noteId) {
        if (!ErrorHandler.isFragmentValid(this)) return;
        if (apiService == null) return;

        Call<NoteResponse> call = apiService.deleteNote(authToken, noteId);
        call.enqueue(new ErrorHandler.SafeCallback<NoteResponse>(this) {
            @Override
            protected void onSuccess(NoteResponse data) {
                ErrorHandler.showToast(getContext(), "删除成功");
                loadNotes();
            }

            @Override
            protected void onError(String message) {
                ErrorHandler.showToast(getContext(), ErrorHandler.getSafeString(message, "删除失败"));
            }
        });
    }
}