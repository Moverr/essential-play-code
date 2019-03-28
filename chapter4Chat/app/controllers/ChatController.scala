package controllers

import javax.inject._
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.mvc._
import play.api.libs.json._
import services._
import services.AuthServiceMessages._
import services.ChatServiceMessages._

@Singleton class ChatController @Inject() (mcc: MessagesControllerComponents, chatService: ChatService, authService: AuthService) extends MessagesAbstractController(mcc) with ControllerHelpers {
  val chatForm = Form(mapping(
    "text" -> nonEmptyText
  )(ChatRequest.apply)(ChatRequest.unapply))

  def index = Action { implicit request: MessagesRequest[AnyContent] =>
    withAuthenticatedUser(request) { creds =>
      chatRoom()
    }
  }

  def submitMessage = Action { implicit request: MessagesRequest[AnyContent] =>
    withAuthenticatedUser(request) { creds =>
      chatForm.bindFromRequest().fold(
        hasErrors = { form: Form[ChatRequest] =>
          chatRoom(form)
        },
        success = { chatReq: ChatRequest =>
          chatService.chat(creds.username, chatReq.text)
          chatRoom(chatForm)
        }
      )
    }
  }

  private def chatRoom(form: Form[ChatRequest] = chatForm)(implicit msgs: Messages): Result =
    Ok(views.html.chatroom(chatService.messages, form))

  private def withAuthenticatedUser(request: Request[AnyContent])(func: Credentials => Result): Result =
    request.sessionCookieId match {
      case Some(sessionId) =>
        authService.whoami(sessionId) match {
          case res: Credentials     => func(res)
          case res: SessionNotFound => redirectToLogin
        }
      case None => redirectToLogin
    }

  private val redirectToLogin: Result =
    Redirect(routes.AuthController.login)
}
