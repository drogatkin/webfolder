package android.provider;
import java.util.Set;
import java.util.HashSet;
import android.content.Context;

public interface MediaStore {
    public static Set<String> getExternalVolumeNames(Context context) {
        
        return new HashSet<String>();
    }
}