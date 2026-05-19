package org.vish.hollowdemo.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.vish.hollowdemo.model.Movie;
import org.vish.hollowdemo.model.User;
import org.vish.hollowdemo.producer.CatalogDataGenerator;
import org.vish.hollowdemo.producer.UserDataGenerator;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class MeasureJsonSize {

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new KotlinModule.Builder().build());

        // ── Catalog ──────────────────────────────────────────────────────────
        System.out.println("Generating 50,000 movies...");
        long t0 = System.currentTimeMillis();
        CatalogDataGenerator catalogGen = new CatalogDataGenerator(50_000);
        List<Movie> movies = catalogGen.generateInitialDataset(50_000);
        System.out.printf("  Generated in %dms%n", System.currentTimeMillis() - t0);

        byte[] catalogJson = mapper.writeValueAsBytes(movies);
        byte[] catalogGzip = gzip(catalogJson);
        long catalogPojoHeap = measurePojoHeap(() -> {
            try { sink = mapper.readValue(catalogJson, mapper.getTypeFactory()
                    .constructCollectionType(List.class, Movie.class)); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        System.out.printf("  Catalog JSON (uncompressed)      : %s%n", humanSize(catalogJson.length));
        System.out.printf("  Catalog JSON (gzipped)           : %s%n", humanSize(catalogGzip.length));
        System.out.printf("  Catalog POJO heap after load     : %s  ← what stays in RAM%n", humanSize(catalogPojoHeap));
        System.out.printf("  Hollow snapshot (S3)             : %s%n", humanSize(10_939_345));
        System.out.printf("  Hollow heap (live)               : %s%n", humanSize(11_936_486));
        System.out.printf("  Hollow delta (one cycle)         : %s%n", humanSize(634_449));
        System.out.println();

        // ── Users ─────────────────────────────────────────────────────────────
        System.out.println("Generating 350,000 users...");
        t0 = System.currentTimeMillis();
        UserDataGenerator userGen = new UserDataGenerator(350_000, 50_000L);
        List<User> users = userGen.generateUsers(350_000);
        System.out.printf("  Generated in %dms%n", System.currentTimeMillis() - t0);

        byte[] usersJson = mapper.writeValueAsBytes(users);
        byte[] usersGzip = gzip(usersJson);
        long usersPojoHeap = measurePojoHeap(() -> {
            try { sink = mapper.readValue(usersJson, mapper.getTypeFactory()
                    .constructCollectionType(List.class, User.class)); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        System.out.printf("  Users JSON (uncompressed)        : %s%n", humanSize(usersJson.length));
        System.out.printf("  Users JSON (gzipped)             : %s%n", humanSize(usersGzip.length));
        System.out.printf("  Users POJO heap after load       : %s  ← what stays in RAM%n", humanSize(usersPojoHeap));
        System.out.printf("  Hollow snapshot (S3)             : %s%n", humanSize(25_882_828));
        System.out.printf("  Hollow heap (live)               : %s%n", humanSize(25_740_104));
        System.out.println();

        // ── Totals ────────────────────────────────────────────────────────────
        System.out.println("── Combined totals ──────────────────────────────────────────────────");
        System.out.printf("  JSON (uncompressed)              : %s%n", humanSize(catalogJson.length + usersJson.length));
        System.out.printf("  JSON (gzipped / wire size)       : %s%n", humanSize(catalogGzip.length + usersGzip.length));
        System.out.printf("  POJO heap after deserialize      : %s  ← actual RAM consumed%n", humanSize(catalogPojoHeap + usersPojoHeap));
        System.out.printf("  Hollow snapshots (S3 / wire)     : %s%n", humanSize(10_939_345 + 25_882_828));
        System.out.printf("  Hollow heap (live)               : %s%n", humanSize(11_936_486 + 25_740_104));
        System.out.printf("  Hollow delta (catalog, one cycle): %s%n", humanSize(634_449));
        System.out.println();
        System.out.println("Key ratios:");
        long pojoTotal = catalogPojoHeap + usersPojoHeap;
        long hollowHeap = 11_936_486L + 25_740_104L;
        System.out.printf("  POJO heap / Hollow heap          : %.1fx larger%n", (double) pojoTotal / hollowHeap);
        System.out.printf("  Gzip wire / Hollow snapshot wire : %.1fx%n",
                (double)(catalogGzip.length + usersGzip.length) / (10_939_345 + 25_882_828));
    }

    // Deserialize, hold the result so GC can't collect it, then measure the retained delta.
    @SuppressWarnings("unused")
    private static volatile Object sink;

    private static long measurePojoHeap(Runnable loader) {
        Runtime rt = Runtime.getRuntime();
        sink = null;
        for (int i = 0; i < 5; i++) { rt.gc(); }
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        long before = rt.totalMemory() - rt.freeMemory();
        loader.run();  // loader must assign into sink to prevent collection
        for (int i = 0; i < 5; i++) { rt.gc(); }
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        long after = rt.totalMemory() - rt.freeMemory();
        return Math.max(after - before, 0);
    }

    private static byte[] gzip(byte[] data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (OutputStream gz = new GZIPOutputStream(bos)) {
            gz.write(data);
        }
        return bos.toByteArray();
    }

    private static String humanSize(long bytes) {
        if (bytes >= 1_073_741_824) return String.format("%.2f GB", bytes / 1_073_741_824.0);
        if (bytes >= 1_048_576)     return String.format("%.2f MB", bytes / 1_048_576.0);
        if (bytes >= 1_024)         return String.format("%.2f KB", bytes / 1_024.0);
        return bytes + " B";
    }
}
