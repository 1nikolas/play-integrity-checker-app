package gr.nikolasspyr.integritycheck.entities;

// Move Method Refactoring
public class StreamUtils {
    // Utility class for stream operations
    public class StreamUtils {
        public static void copyStream(InputStream from, OutputStream to) throws IOException {
            byte[] buf = new byte[1024 * 1024];
            int len;
            while ((len = from.read(buf)) > 0) {
                to.write(buf, 0, len);
            }
        }

        public static byte[] readStream(InputStream inputStream) throws IOException {
            try (InputStream in = inputStream) {
                return readStreamNoClose(in);
            }
        }

        public static String readStream(InputStream inputStream, String charset) throws IOException {
            return new String(readStream(inputStream), charset);
        }

        public static byte[] readStreamNoClose(InputStream inputStream) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            copyStream(inputStream, buffer);
            return buffer.toByteArray();
        }
    }

}
