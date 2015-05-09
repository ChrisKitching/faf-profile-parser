import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static ByteBuffer readFully(String fileName) throws IOException {
        Path path = FileSystems.getDefault().getPath(fileName);

        ByteBuffer bytes = ByteBuffer.wrap(Files.readAllBytes(path));
        bytes.order(ByteOrder.LITTLE_ENDIAN);

        return bytes;
    }

    private static String readString(ByteBuffer fromBuffer) {
        // Read a pascal style string from the buffer.
        short length = fromBuffer.getShort();

        byte[] stringBuffer = new byte[length];
        fromBuffer.get(stringBuffer, 0, length);

        return new String(stringBuffer);
    }

    public static void main(String[] args) throws IOException {
        ByteBuffer keyFile = readFully(args[0]);
        ByteBuffer profileFile = readFully(args[1]);

        ByteBuffer outputBuffer = ByteBuffer.allocateDirect(10000000);
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Write the keyfile
        outputBuffer.put(getKeyfileHeader());

        while (keyFile.hasRemaining()) {
            int methodId = (int) (keyFile.getLong() << 2);

            // Because the traceview format is fucking stupid...
            String hexMethodId = "0x" + Integer.toHexString(methodId);

            String classAndMethodName[] = readString(keyFile).split(" ");


            // The class name is the prefix of the array....
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < classAndMethodName.length - 1; i++) {
                sb.append(classAndMethodName[i]);
                sb.append(" ");
            }
            sb.setLength(sb.length() - 1);
            String className = sb.toString();

            // And the method name is the last one.
            String methodName = classAndMethodName[classAndMethodName.length - 1];

            outputBuffer.put(hexMethodId.getBytes());
            outputBuffer.put("\t".getBytes());
            outputBuffer.put(className.getBytes());
            outputBuffer.put("\t".getBytes());
            outputBuffer.put(methodName.getBytes());

            // A default signature to keep the parser happy.
            outputBuffer.put("\t()V\n".getBytes());
        }

        outputBuffer.put("*end\n".getBytes());
        writeFileHeader(outputBuffer);

        // Write the profile records
        while (profileFile.hasRemaining()) {
            // Read the next three Longs from the profile file
            int methodId = (int) profileFile.getLong();
            int actionCode = (int) profileFile.getLong();

            if (actionCode != 0 && actionCode != 1) {
                System.err.println("AAAAAAAAAAAA: " + actionCode);
            }

            int eventTime = (int) profileFile.getLong();

            /* The record format is:
             * u1 thread ID
             * u4 method ID | method action
             * u4 time delta since start, in usec
             */

            // Stick the isCall flag in the least significant bit of methodId.
            methodId = (methodId << 2) | actionCode;

            outputBuffer.put((byte) 1);
            outputBuffer.putInt(methodId);
            outputBuffer.putInt(eventTime);
        }

        File file = new File("out.trace");
        FileChannel channel = new FileOutputStream(file, false).getChannel();
        outputBuffer.flip();
        channel.write(outputBuffer);
        channel.close();
    }

    private static void writeFileHeader(ByteBuffer buffer) {
        buffer.putInt(0x574f4c53);
        buffer.putShort((short) 1);
        buffer.putShort((short) 16);

        long time = System.currentTimeMillis();
        buffer.putLong(time);
    }

    public static byte[] getKeyfileHeader() {
        return (
            "*version\n" +
            "1\n" +
            "clock=global\n" +
            "*threads\n" +
            "1 main\n" +
            "*methods\n"
        ).getBytes();
    }
}
