/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.secrets.api.EntropyChecker;

public class NotBinaryFilePredicate implements FilePredicate {

  private static final Set<String> DEFAULT_BINARY_EXTENSIONS = new HashSet<>(Arrays.asList(
    "3dm",
    "3ds",
    "3g2",
    "3gp",
    "7z",
    "a",
    "aac",
    "aar",
    "adp",
    "ai",
    "aif",
    "aiff",
    "alz",
    "amr",
    "ape",
    "apk",
    "appimage",
    "ar",
    "arj",
    "asf",
    "at",
    "au",
    "avi",
    "b",
    "bak",
    "baml",
    "bfc",
    "bh",
    "bin",
    "bk",
    "bmp",
    "br",
    "btif",
    "bytes",
    "bz2",
    "bzip2",
    "cab",
    "caf",
    "cer",
    "cfe",
    "cfs",
    "cgm",
    "changesubtype",
    "ckp",
    "class",
    "cmx",
    "cpio",
    "cr2",
    "cur",
    "dat",
    "data",
    "db",
    "dcm",
    "deb",
    "dex",
    "dii",
    "dim",
    "djvu",
    "dll",
    "dmg",
    "dng",
    "doc",
    "docm",
    "docx",
    "dot",
    "dotm",
    "dra",
    "ds_store",
    "dsk",
    "dts",
    "dtshd",
    "dvb",
    "dvd",
    "dvm",
    "dwg",
    "dxf",
    "dylib",
    "ecelp4800",
    "ecelp7470",
    "ecelp9600",
    "egg",
    "enc",
    "eol",
    "eot",
    "eps",
    "epub",
    "exe",
    "exec",
    "f4v",
    "fbs",
    "fdm",
    "fdt",
    "fdx",
    "fe",
    "fh",
    "fla",
    "flac",
    "flatpak",
    "fli",
    "flv",
    "fnm",
    "fpx",
    "fst",
    "fvt",
    "g3",
    "gch",
    "gem",
    "gem",
    "gh",
    "gif",
    "gpg",
    "graffle",
    "gz",
    "gzip",
    "h261",
    "h263",
    "h264",
    "heif",
    "hmap",
    "icns",
    "ico",
    "idx",
    "ief",
    "img",
    "ipa",
    "ir",
    "iso",
    "jar",
    "jce",
    "jce",
    "jks",
    "jks",
    "jnilib",
    "jpeg",
    "jpg",
    "jpgv",
    "jpm",
    "jxr",
    "kdbx",
    "kdd",
    "kdi",
    "kdm",
    "key",
    "keystore",
    "keystream",
    "kjsm",
    "kotlin_module",
    "ktx",
    "ldf",
    "lha",
    "lib",
    "lvp",
    "lz",
    "lzh",
    "lzma",
    "lzo",
    "m3u",
    "m4a",
    "m4v",
    "macho32",
    "macho64",
    "mar",
    "mdf",
    "mdi",
    "meta",
    "mht",
    "mid",
    "midi",
    "mj2",
    "mka",
    "mkv",
    "mmdb",
    "mmr",
    "mng",
    "mo",
    "mobi",
    "mobileprovision",
    "mov",
    "movie",
    "mp3",
    "mp4",
    "mp4a",
    "mpeg",
    "mpg",
    "mpga",
    "mxu",
    "nef",
    "nes",
    "nib",
    "node",
    "npx",
    "npz",
    "numbers",
    "nupkg",
    "nvd",
    "nvm",
    "o",
    "odp",
    "ods",
    "odt",
    "oga",
    "ogg",
    "ogv",
    "otf",
    "ott",
    "p12",
    "pages",
    "pbm",
    "pcap",
    "pch",
    "pcx",
    "pdb",
    "pdf",
    "pea",
    "pf",
    "pfx",
    "pgm",
    "phar",
    "pic",
    "pkcs12",
    "pkenc",
    "pkg",
    "pkl",
    "plaso",
    "plist",
    "png",
    "pnm",
    "pos",
    "pot",
    "potm",
    "potx",
    "ppa",
    "ppam",
    "ppm",
    "pps",
    "ppsm",
    "ppsx",
    "ppt",
    "pptm",
    "pptx",
    "proto",
    "protobuf",
    "ps",
    "psd",
    "pxm",
    "pya",
    "pyc",
    "pyo",
    "pyv",
    "qt",
    "rar",
    "ras",
    "raw",
    "res",
    "resources",
    "rgb",
    "rip",
    "rlc",
    "rmf",
    "rmvb",
    "rpm",
    "rsc",
    "rtf",
    "rz",
    "s3m",
    "s7z",
    "scc",
    "scpt",
    "sgi",
    "shar",
    "si",
    "signature",
    "sil",
    "sketch",
    "slk",
    "smv",
    "snap",
    "snk",
    "so",
    "sqlite",
    "sqlite3",
    "st",
    "stack2",
    "stl",
    "sub",
    "suo",
    "swc",
    "swf",
    "tab",
    "tab_i",
    "tar",
    "tbz",
    "tbz2",
    "tga",
    "tgz",
    "thmx",
    "tif",
    "tiff",
    "tim",
    "tip",
    "tlog",
    "tlz",
    "tmd",
    "truststore",
    "ttc",
    "ttf",
    "tvd",
    "tvm",
    "tvx",
    "txz",
    "typedefs",
    "ucfg",
    "ucfgs",
    "udf",
    "uvh",
    "uvi",
    "uvm",
    "uvp",
    "uvs",
    "uvu",
    "viv",
    "vob",
    "war",
    "wasm",
    "wav",
    "wax",
    "wbmp",
    "wdp",
    "weba",
    "webm",
    "webp",
    "whl",
    "wim",
    "wm",
    "wma",
    "wmv",
    "wmx",
    "woff",
    "woff2",
    "wrm",
    "wvx",
    "xbm",
    "xcf",
    "xcuserstate",
    "xif",
    "xla",
    "xlam",
    "xls",
    "xlsb",
    "xlsm",
    "xlsx",
    "xlt",
    "xltm",
    "xltx",
    "xm",
    "xmind",
    "xpi",
    "xpm",
    "xwd",
    "xz",
    "z",
    "zip",
    "zipx",
    "zstd"));

  private static final List<String> DEFAULT_BINARY_SUFFIXES = List.of("cacerts");

  private static final Pattern HEX_REGEX = Pattern.compile("\\p{XDigit}++");
  private static final double MD5_AND_SHA_MIN_ENTROPY = 3.1;

  private final Set<String> binaryFileExtensions;
  private final List<String> binaryFileSuffixes;

  public NotBinaryFilePredicate(String... additionalBinarySuffixes) {
    binaryFileExtensions = new HashSet<>(DEFAULT_BINARY_EXTENSIONS);
    binaryFileSuffixes = new ArrayList<>(DEFAULT_BINARY_SUFFIXES);
    List<String> cleanedSuffixes = Arrays.stream(additionalBinarySuffixes)
      .map(String::trim)
      .filter(value -> !value.isEmpty())
      .distinct()
      .collect(Collectors.toList());
    for (String suffix : cleanedSuffixes) {
      boolean isExtension = suffix.length() > 1 && suffix.startsWith(".") && suffix.indexOf('.', 1) == -1;
      if (isExtension) {
        binaryFileExtensions.add(suffix.substring(1));
      } else {
        binaryFileSuffixes.add(suffix);
      }
    }
  }

  @Override
  public boolean apply(InputFile inputFile) {
    String filename = inputFile.filename();
    String extension = extension(filename);
    boolean hasBinaryExtension = extension != null && binaryFileExtensions.contains(extension);
    return !hasBinaryExtension &&
      binaryFileSuffixes.stream().noneMatch(filename::endsWith) &&
      !isMd5OrSha1(filename);
  }

  public boolean isMd5OrSha1(String filename) {
    int len = filename.length();
    return ( /* md5 */ len == 32 || /* sha1 */ len == 40 || /* sha256 */ len == 64 || /* sha512 */ len == 128) &&
      HEX_REGEX.matcher(filename).matches() && EntropyChecker.calculateShannonEntropy(filename) > MD5_AND_SHA_MIN_ENTROPY;

  }

  public void addBinaryFileExtension(String extension) {
    binaryFileExtensions.add(extension);
  }

  @Nullable
  public static String extension(String filename) {
    int dotPos = filename.lastIndexOf('.');
    if (dotPos == -1 || dotPos == filename.length() - 1) {
      return null;
    }
    return filename.substring(dotPos + 1).toLowerCase(Locale.ROOT);
  }

}
