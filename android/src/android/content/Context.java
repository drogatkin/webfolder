package android.content;

public interface Context {
    public static final String STORAGE_SERVICE = "";
    
    Object getSystemService(String name);
}