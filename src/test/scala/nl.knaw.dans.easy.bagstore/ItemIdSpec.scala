/**
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.bagstore

import java.nio.file.Paths
import java.util.UUID


import scala.util.{ Failure, Success }

class ItemIdSpec extends TestSupportFixture {
  private val uuid: UUID = UUID.randomUUID()

  import ItemId._

  "fromString" should "return a Failure if string is empty" in {
    fromString("") shouldBe a[Failure[_]]
  }

  it should "return a bag-id if string is a valid UUID" in {
    val uuid = UUID.randomUUID()

    inside(fromString(s"$uuid")) {
      case Success(BagId(id)) => id shouldBe uuid
    }
  }

  it should "return a Failure if string is an invalid UUID" in {
    fromString(s"${ UUID.randomUUID() }-INVALID") shouldBe a[Failure[_]]
  }

  it should "return a file-id with correct components if a path found after UUID" in {
    val uuid = UUID.randomUUID()

    inside(fromString(s"$uuid/path/to/file")) {
      case Success(FileId(BagId(id), path, _)) =>
        id shouldBe uuid
        path shouldBe Paths.get("path/to/file")
    }
  }

  it should "return a file-id (NOT bag-id) if empty path found after revision" in {
    val uuid = UUID.randomUUID()
    inside(fromString(s"$uuid/")) {
      case Success(FileId(BagId(id), path, _)) =>
        id shouldBe uuid
        path shouldBe Paths.get("")
    }
  }

  it should "percent-decode path" in {
    val uuid = UUID.randomUUID()

    inside(fromString(s"$uuid/path/to/file%20with%20spaces")) {
      case Success(FileId(BagId(id), path, _)) =>
        id shouldBe uuid
        path shouldBe Paths.get("path/to/file with spaces")
    }
  }

  "fromString" should "not trigger an IllegalArgumentException when presented a valid uuid" in {
    fromString(uuid.toString) should matchPattern {
      case Success(BagId(`uuid`)) =>
    }
  }

  it should "trigger an IllegalArgumentException when presented a too long UUID at the end should" in {
    val tooLongUuidAtEnd = "1234abcd-12AB-12ab-12AB-123456abcdef1278713487134"
    expectValidationToFailOnUuidLength(tooLongUuidAtEnd)
  }

  it should "trigger an IllegalArgumentException when presented a too long UUID at the start should" in {
    val tooLongUuidAtStart = "12787134871341234abcd-12AB-12ab-12AB-123456abcdef"
    expectValidationToFailOnUuidLength(tooLongUuidAtStart)
  }

  it should "trigger an IllegalArgumentException when presented a nonsense UUID with the right amount of characters" in {
    val nonsenseUuid = "a badly formatted uuid with 36 chars"
    fromString(nonsenseUuid) shouldBe a[Failure[_]]
  }

  it should "trigger an IllegalArgumentException when presented an UUID with special characters" in {
    val uuidWithExclamationMark = "1234abcd-12AB-12ab-12AB-123456abcde!"
    fromString(uuidWithExclamationMark) should matchPattern {
      case Failure(e: NumberFormatException) if e.getMessage.equals("""For input string: "123456abcde!"""") =>
    }
  }

  it should "trigger an IllegalArgumentException when presented a too short UUID at start" in {
    val tooShortUuidAtStart = "bcd-12AB-12ab-12AB-123456abcdef"
    expectValidationToFailOnUuidLength(tooShortUuidAtStart)
  }

  it should "trigger an IllegalArgumentException when presented a too short UUID at the end" in {
    val tooShortUuidAtEnd = "1234abcd-12AB-12ab-12AB-123456abc"
    expectValidationToFailOnUuidLength(tooShortUuidAtEnd)
  }

  "BagId.toString" should "print UUID" in {
    BagId(uuid).toString shouldBe uuid.toString
  }

  "FileId.toString" should "print bag-id/filename" in {
    FileId(uuid, Paths.get("filename")).toString shouldBe s"${ uuid.toString }/filename"
  }

  it should "percent-encode spaces in the filepath" in {
    FileId(uuid, Paths.get("path with/some spaces")).toString shouldBe s"${ uuid.toString }/path%20with/some%20spaces"
  }

  it should "percent-encode funny characters in the filepath" in {
    /*
     * Here, we calculate how the given code point be percent-encoded. This is just a sanity check. We should actually rely on the Guave library to get this right.
     */
    val encodedBytes = "\u2D10".getBytes("UTF-8").map("%" + Integer.toHexString(_).takeRight(2).toUpperCase).mkString("")
    FileId(uuid, Paths.get("path/with/Georgian/char/here/\u2D10")).toString shouldBe s"${ uuid.toString }/path/with/Georgian/char/here/$encodedBytes"
  }

  "ItemId.toFileId" should "fail when passed a BagId" in {
    val bagId = BagId(uuid)

    inside(bagId.toFileId) {
      case Failure(NoFileIdException(id)) => id shouldBe bagId
    }
  }

  it should "succeed when passed a FileId" in {
    val fileId = FileId(uuid, Paths.get("some/path"))

    inside(fileId.toFileId) {
      case Success(f) => f shouldBe fileId
    }
  }

  "ItemId.toBagId" should "fail when passed a FileId" in {
    val fileId = FileId(uuid, Paths.get("some/path"))

    inside(fileId.toBagId) {
      case Failure(NoBagIdException(id)) => id shouldBe fileId
    }
  }

  it should "succeed when passed a BagId" in {
    val bagId = BagId(uuid)

    inside(bagId.toBagId) {
      case Success(b) => b shouldBe bagId
    }
  }

  private def expectValidationToFailOnUuidLength(nonsenseUuid: String) = {
    fromString(nonsenseUuid) should matchPattern {
      case Failure(e: IllegalArgumentException) if e.getMessage.contains(s"A UUID should contain exactly 36 characters, this UUID has ${ nonsenseUuid.length } characters") =>
    }
  }
}
