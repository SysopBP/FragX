package df.root;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReportRedactorTest {
    @Test public void removesIdentifiersAndLeavesFailureUseful() {
        String output = ReportRedactor.redact("serial: ABC123\nIMEI=123456789012345\n--aes-key: deadbeef\nemail: someone@example.com\npeer 192.168.1.10 aa:bb:cc:dd:ee:ff\nbootstrap failed exit 2\nKernel: 6.12.69-android16\n");
        for (String secret : new String[]{"ABC123", "123456789012345", "deadbeef", "someone@example.com", "192.168.1.10", "aa:bb:cc:dd:ee:ff"}) assertFalse(secret, output.contains(secret));
        assertTrue(output.contains("bootstrap failed exit 2"));
        assertTrue(output.contains("Kernel: 6.12.69-android16"));
    }
    @Test public void preservesMultilineStructure() {
        assertEquals("token: [redacted]\nRoot: active\n", ReportRedactor.redact("token: abc\nRoot: active\n"));
    }
}
