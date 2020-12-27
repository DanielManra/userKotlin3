package Library;

import android.os.Build;
import android.util.Pair;
import android.widget.EditText;
import androidx.annotation.RequiresApi;
import androidx.databinding.BindingAdapter;
import com.example.userkotlin.R;
import Models.BindableString;


public class BindEditText {
    @BindingAdapter({"app:binding"})
    public static void bindEditText (EditText view, final BindableString bindableString){
        Pair<BindableString, TextWatcherAdapter> pair = (Pair) view.getTag(R.id.bound_observable);
        if (pair == null || pair.first != bindableString){
            if (pair != null){
                view.removeTextChangedListener(pair.second);
            }
            TextWatcherAdapter watcher = new TextWatcherAdapter(){
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                public void onTextChanged(CharSequence s, int start, int before, int count){
                    bindableString.setValue(s.toString());
                }
            };
            view.setTag(R.id.bound_observable, new Pair<>(bindableString, watcher));
            view.addTextChangedListener(watcher);
        }
        String newValue = bindableString.getValue();
        if (!view.getText().toString().equals(newValue)){
            view.setText(newValue);
        }
    }
}
