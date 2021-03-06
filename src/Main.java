import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Stack;

public class Main {
    private static HashMap<Integer, String> methodNames = new HashMap<>();

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

        // Nothing to see here.
        KHANG.seed(new File(args[0]).length() * new File(args[1]).length());

        // Holds the complete output.
        ByteBuffer outputBuffer = ByteBuffer.allocate(100000000);

        ByteBuffer keyfileContentBuffer = ByteBuffer.allocate(100000000);

        // Buffers the profile records.
        ByteBuffer profileBuffer = ByteBuffer.allocate(100000000);

        outputBuffer.order(ByteOrder.LITTLE_ENDIAN);
        profileBuffer.order(ByteOrder.LITTLE_ENDIAN);
        keyfileContentBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Write the methods portion of the keyfile.
        writeKeyfile(keyfileContentBuffer, keyFile);
        keyfileContentBuffer.flip();

        // We need an additional scan to determine the maximum threadId before we can complete
        // writing the actual keyfile, so we do the profile scan early.
        int maxThreadId = writeProfileRecords(profileBuffer, profileFile);
        profileBuffer.flip();

        // Armed with the thread count, we can write the output file.
        outputBuffer.put(getKeyfileHeader());
        writeThreadKeymap(outputBuffer, maxThreadId);

        // Write the methods portion of the keyfile.
        outputBuffer.put("*methods\n".getBytes());
        outputBuffer.put(keyfileContentBuffer);

        // Write the end of the keyfile.
        outputBuffer.put("*end\n".getBytes());

        // Write the trace file (which we"ve already buffered in profileBuffer)
        writeFileHeader(outputBuffer);
        outputBuffer.put(profileBuffer);

        File file = new File("out.trace");
        FileChannel channel = new FileOutputStream(file, false).getChannel();
        outputBuffer.flip();
        channel.write(outputBuffer);
        channel.close();
    }

    private static void writeThreadKeymap(ByteBuffer outputBuffer, int numThreads) {
        if (numThreads > 65536) {
            System.err.println("The traceview format cannot handle >65536 threads!");
            System.exit(1);
            return;
        }

        // Synthesize a thread name for every thread.
        for (int i = 0; i < numThreads; i++) {
            outputBuffer.put((Integer.toString(i) + "\t" + KHANG.getName() + "\n").getBytes());
        }

    }

    private static void writeKeyfile(ByteBuffer keyfileBuffer, ByteBuffer keyFile) {
        while (keyFile.hasRemaining()) {
            int rawMethodId = (int) keyFile.getLong();
            int methodId = rawMethodId << 2;

            // Because the traceview format is fucking stupid...
            String hexMethodId = "0x" + Integer.toHexString(methodId);

            String prettyName = readString(keyFile);
            String[] classAndMethodName = prettyName.split("\t");

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

            methodNames.put(rawMethodId, prettyName);

            keyfileBuffer.put(hexMethodId.getBytes());
            keyfileBuffer.put("\t".getBytes());
            keyfileBuffer.put(className.getBytes());
            keyfileBuffer.put("\t".getBytes());
            keyfileBuffer.put(methodName.getBytes());

            // A default signature to keep the parser happy.
            keyfileBuffer.put("\t()V\n".getBytes());
        }

        keyFile.rewind();
    }

    /**
     * Write the profile records in profileFile to the given output buffer.
     * Verifies that the stack makes sense as it goes along.
     */
    private static int writeProfileRecords(ByteBuffer profileBuffer, ByteBuffer profileFile) {
        int maxThreadID = 0;

        // For each thread, track the methodId of the executing function. Used to verify if the
        // call stack makes sense.
        HashMap<Short, Stack<Integer>> callStacks = new HashMap<>();

        // Write the profile records
        while (profileFile.hasRemaining()) {
            // Read the next three Longs from the profile file
            short threadId = (short) profileFile.getLong();
            int methodId = (int) profileFile.getLong();
            int actionCode = (int) profileFile.getLong();
            int threadTime = (int) profileFile.getLong();
            int globalTime = (int) profileFile.getLong();

            if (threadId > maxThreadID) {
                maxThreadID = threadId;
            }

            // Get or create the callstack for the current thread.
            Stack<Integer> callStack = callStacks.get(threadId);
            if (callStack == null) {
                callStack = new Stack<>();
                callStacks.put(threadId, callStack);
            }

            if (actionCode == 0) {
                // Method entry
                callStack.push(methodId);
            } else if (actionCode == 1) {
                // Method exit. Verify that we"re popping a value equal to methodId, otherwise we"re
                // trying to return from the wrong function...

                if (!callStack.isEmpty()) {
                    int topOfStack = callStack.pop();
                    if (topOfStack != methodId) {
                        System.err.println("Thread:" + threadId);
                        for (Integer integer : callStack) {
                            System.err.println("    (" + integer + "): " + methodNames.get(integer));
                        }

                        System.err.println("Top of stack method: " + methodNames.get(topOfStack) + " (" + topOfStack + ")");
                        System.err.println("This record returns: " + methodNames.get(methodId) + " (" + methodId + ")");

                        System.exit(3);
                        return -1;
                    }
                } else {
                    continue;
                }
            } else {
                // PANIC
                System.err.println("Unexpected actionCode in bagging area: " + actionCode);
                System.exit(2);
                return -1;
            }

            /* The record format is:
             * u2 thread ID
             * u4 method ID | method action
             * u4 time delta since start, in usec
             */

            // Stick the isCall flag in the least significant bit of methodId.
            methodId = (methodId << 2) | actionCode;

            profileBuffer.putShort(threadId);
            profileBuffer.putInt(methodId);
            profileBuffer.putInt(threadTime);
            profileBuffer.putInt(globalTime);
        }

        profileFile.rewind();

        return maxThreadID;
    }

    private static void writeFileHeader(ByteBuffer buffer) {
        buffer.putInt(0x574f4c53);   // Magic
        buffer.putShort((short) 2);  // Version number
        buffer.putShort((short) 16); // 16 (required)

        long time = System.currentTimeMillis();
        buffer.putLong(time);
    }

    public static byte[] getKeyfileHeader() {
        return (
            "*version\n" +
            "2\n" +
            "clock=dual\n" +
            "*threads\n"
        ).getBytes();
    }
}
