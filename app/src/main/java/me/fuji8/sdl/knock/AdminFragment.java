package me.fuji8.sdl.knock;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import me.fuji8.sdl.knock.databinding.FragmentAdminBinding;
import me.fuji8.sdl.knock.message.ChatMessage;

public class AdminFragment extends Fragment {

    private FragmentAdminBinding binding;
    public static ListView logview;

    public static final ArrayList<ChatMessage> chatLog = new ArrayList<>();
    public static ArrayAdapter<ChatMessage> chatLogAdapter;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentAdminBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(AdminFragment.this)
                        .navigate(R.id.action_AdminFragment_to_FirstFragment);
            }
        });

        // log view
        chatLogAdapter = new ArrayAdapter<ChatMessage>(getActivity(),0, chatLog) {
            @Override
            public @NonNull
            View getView(int pos, @Nullable View view, @NonNull ViewGroup parent) {
                if (view == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                ChatMessage message = getItem(pos);
                assert message != null;
                TextView text1 = view.findViewById(android.R.id.text1);
                if (message.sender != null) {
                    text1.setTextColor(message.sender.equals(MainActivity.adapter.getName()) ? Color.GRAY : Color.BLACK);
                }
                text1.setText(message.content);
                return view;
            }
        };
        logview = binding.logviewFirst;
        logview.setAdapter(chatLogAdapter);
        final DateFormat fmt = DateFormat.getDateTimeInstance();
        logview.setOnItemClickListener((parent, v, pos, id) -> {
            ChatMessage msg = (ChatMessage) parent.getItemAtPosition(pos);
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.msg_title, msg.seq, msg.sender))
                    .setMessage(getString(R.string.msg_content, msg.content, fmt.format(new Date(msg.time))))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}