package example.spring.boot.domains.books.business

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectResult
import com.amazonaws.services.s3.model.S3Object
import io.mockk.mockk
import java.io.InputStream

typealias BookId = String
typealias BucketName = String

class MockS3(private val s3: AmazonS3 = mockk<AmazonS3>()) : AmazonS3 by s3 {

    private val store: MutableMap<BookId, SavedObject> = HashMap()

    override fun putObject(
        bucketName: BucketName,
        key: BookId,
        bytes: InputStream,
        metadata: ObjectMetadata,
    ): PutObjectResult {
        store[key] = SavedObject(String(bytes.readAllBytes()), metadata.contentType, metadata.contentLength)

        return PutObjectResult()
    }

    override fun getObject(bucketName: BucketName, key: BookId): S3Object {
        val savedObject = store[key] ?: throw AmazonS3Exception("Not found").apply { statusCode = 404 }

        return S3Object().also { s3Object ->
            s3Object.bucketName = bucketName
            s3Object.key = key
            s3Object.setObjectContent(savedObject.bytes.byteInputStream())
            s3Object.objectMetadata = ObjectMetadata().also { meta ->
                meta.contentLength = savedObject.contentLength
                meta.contentType = savedObject.contentType
            }
        }
    }

    data class SavedObject(
        val bytes: String,
        val contentType: String,
        val contentLength: Long,
    )
}