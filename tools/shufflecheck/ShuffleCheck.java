import java.util.Arrays;
import java.util.Random;

/** 与 LessonScreen.kt 中的两个打乱函数逐行等价（含「按哈希旋转」修正），用于交叉校验 JS 复刻实现。 */
public class ShuffleCheck {
    static long seedSingle(int hash) { return ((long) hash) * 1000003L ^ 0x5DEECE66DL; }
    static long seedOrder(int hash)  { return ((long) hash) * 31L ^ 0x2545F4914F6CDD1DL; }

    static int[] perm(int n, long seed) {
        Random rnd = new Random(seed);
        int[] idx = new int[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        for (int i = n - 1; i >= 1; i--) {
            int j = rnd.nextInt(i + 1);
            int t = idx[i]; idx[i] = idx[j]; idx[j] = t;
        }
        return idx;
    }

    /** 若等于正确顺序，则按哈希循环左移 k 位（k = 1 + floorMod(hash, n-1)）。 */
    static int[] rotIfIdentity(int hash, int[] idx) {
        boolean same = true;
        for (int i = 0; i < idx.length; i++) if (idx[i] != i) { same = false; break; }
        if (same) {
            int k = 1 + Math.floorMod(hash, idx.length - 1);
            for (int r = 0; r < k; r++) {
                int f = idx[0];
                System.arraycopy(idx, 1, idx, 0, idx.length - 1);
                idx[idx.length - 1] = f;
            }
        }
        return idx;
    }

    public static void main(String[] a) {
        int[] hashes = {0, 42, -1, 123456789, -987654321, 2147483647, -2147483648,
                        55555555, -42, 1000000007, -1000000007, 7};
        for (int h : hashes) {
            int[] s = perm(4, seedSingle(h));
            int[] o = rotIfIdentity(h, perm(4, seedOrder(h)));
            System.out.println("H " + h + " S " + Arrays.toString(s) + " O " + Arrays.toString(o));
        }
        for (int h : new int[]{7, -5, 3, 5, -3, 0}) {
            int[] id = {0, 1, 2, 3};
            System.out.println("R " + h + " " + Arrays.toString(rotIfIdentity(h, id)));
        }
    }
}
