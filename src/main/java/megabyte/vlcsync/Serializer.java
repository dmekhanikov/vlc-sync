package megabyte.vlcsync;

public class Serializer {

    private static final int TIME_SIZE = 4;
    private static final int MOD = 256;

    private Serializer() {}

    public static byte[] encodeTime(long time) {
        byte[] data = new byte[TIME_SIZE];
        for (int i = 0; time != 0; i++) {
            data[i] = (byte) (time % MOD);
            time /= MOD;
        }
        return data;
    }

    public static long decodeTime(byte[] data) {
        long time = 0;
        for (int i = TIME_SIZE - 1; i >= 0; i--) {
            time *= MOD;
            int r = (data[i] + MOD) % MOD;
            time += r;
        }
        return time;
    }
}
