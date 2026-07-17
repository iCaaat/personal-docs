package com.luckyby.personaldocs.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OfficePreviewService {
  private static final Set<String> OFFICE_EXTENSIONS = Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx");
  private final Path previewRoot;
  private final String libreOfficeCommand;
  private final long timeoutSeconds;

  public OfficePreviewService(@Value("${app.storage.path}") String storage,
                              @Value("${app.preview.libre-office-command}") String libreOfficeCommand,
                              @Value("${app.preview.timeout-seconds}") long timeoutSeconds) throws IOException {
    this.previewRoot = Paths.get(storage).toAbsolutePath().normalize().resolve("previews");
    this.libreOfficeCommand = libreOfficeCommand;
    this.timeoutSeconds = timeoutSeconds;
    Files.createDirectories(previewRoot);
  }

  public boolean supports(Document document) { return OFFICE_EXTENSIONS.contains(extension(document.getOriginalName())); }

  public Path preview(Document document, Path source) {
    if ("pdf".equals(extension(document.getOriginalName()))) return source;
    if (!supports(document)) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "该文件格式暂不支持在线预览");
    Path target = previewRoot.resolve(stripExtension(document.getStorageKey()) + ".pdf");
    if (Files.exists(target)) return target;
    try {
      Process process = new ProcessBuilder(libreOfficeCommand, "--headless", "--convert-to", "pdf", "--outdir", previewRoot.toString(), source.toString())
          .redirectErrorStream(true).start();
      boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
      if (!completed) { process.destroyForcibly(); throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "文档转换超时"); }
      if (process.exitValue() != 0 || !Files.exists(target)) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "文档转换失败，请确认服务器已安装 LibreOffice");
      return target;
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "无法启动 LibreOffice，请检查服务器配置", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "文档转换已中断", e);
    }
  }

  public void deletePreview(Document document) throws IOException { Files.deleteIfExists(previewRoot.resolve(stripExtension(document.getStorageKey()) + ".pdf")); }
  private String extension(String name) { int index = name.lastIndexOf('.'); return index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT); }
  private String stripExtension(String name) { int index = name.lastIndexOf('.'); return index < 0 ? name : name.substring(0, index); }
}
