/*
 * This file is part of FragPipe.
 *
 * FragPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FragPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FragPipe. If not, see <https://www.gnu.org/licenses/>.
 */

package com.dmtavt.fragpipe.messages;

import com.github.chhh.utils.PathUtils;
import com.github.chhh.utils.TimeUtils;
import java.nio.file.Files;
import java.nio.file.Path;

public class MessageSaveLog {
  public final Path workDir;

  public MessageSaveLog(Path filePath) {
    this.workDir = filePath;
  }

  public static MessageSaveLog saveInDir(Path dir) {
    Path existing = PathUtils.existing(dir.toString());
    if (existing == null) {
      throw new IllegalArgumentException("Given dir must exist");
    }
    if (!Files.isDirectory(existing)) {
      throw new IllegalArgumentException("Given mpath must be a dir");
    }
    return new MessageSaveLog(existing.resolve(String.format("log_%s.txt", TimeUtils.dateTimeNoSpaces())));
  }
}
