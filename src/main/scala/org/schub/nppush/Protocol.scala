package org.schub.nppush

import spray.httpx.marshalling._
import spray.http.MediaTypes._

/**
 *
 */
object Protocol {

  case object Tick

  case object GetPadContent

  case class GetBlogPost(blogId: String, username: String, password: String, postId: String)

  object GetBlogPost {
    implicit val GetBlogPostMarshaller =
      Marshaller.delegate[GetBlogPost, String](`text/xml`, `application/xml`)(post => {
        """<methodCall>
      <methodName>wp.getPost</methodName>
      <params>
        <param>
          <key>blog_id</key>
          <value>%s</value>
        </param>
        <param>
          <key>username</key>
          <value>%s</value>
        </param>
        <param>
          <key>password</key>
          <value>%s</value>
        </param>
        <param>
          <key>post_id</key>
          <value>%s</value>
        </param>
      </params>
    </methodCall>""".format(post.blogId, post.username, post.password, post.postId)
      })
  }

  case class PadContent(content: String)

  case class BlogPostContent(content: String)

  case class UpdateBlogPost(blogId: String,
                            username: String,
                            password: String,
                            postId: String,
                            content: BlogPostContent)

  object UpdateBlogPost {
    implicit val UpdateBlogPostMarshaller = {
      Marshaller.delegate[UpdateBlogPost, String](`text/xml`, `application/xml`)(post => {
        """<methodCall>
      <methodName>wp.editPost</methodName>
      <params>
        <param>
          <key>blog_id</key>
          <value>%s</value>
        </param>
        <param>
          <key>username</key>
          <value>%s</value>
        </param>
        <param>
          <key>password</key>
          <value>%s</value>
        </param>
        <param>
          <key>post_id</key>
          <value>%s</value>
        </param>
        <param>
          <key>content</key>
          <value>
            <struct>
              <member>
                <name>post_content</name>
                <value>
                  <string>%s</string>
                </value>
              </member>
            </struct>
          </value>
        </param>
      </params>
    </methodCall>""".format(post.blogId, post.username, post.password, post.postId, post.content.content)
      })
    }
  }

  case class BlogPost(id: String)

}
