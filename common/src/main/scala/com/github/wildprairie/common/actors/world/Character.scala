package com.github.wildprairie.common.actors.world

import akka.actor.Props
import com.github.wakfutcp.protocol.raw.CharacterDataSet.ForCharacterListSet

/**
  * Created by jacek on 20.05.17.
  */
object Character {
  def props(id: Long, ownerId: Long): Props =
    Props(classOf[Character], id, ownerId)

  def props(data: CharacterCreationData, ownerId: Long): Props =
    Props(classOf[Character], data, ownerId)

  final case class CharacterCreationData(
    id: Long,
    sex: Byte,
    skinColorIndex: Byte,
    hairColorIndex: Byte,
    pupilColorIndex: Byte,
    skinColorFactor: Byte,
    hairColorFactor: Byte,
    clothIndex: Byte,
    faceIndex: Byte,
    breed: Short,
    name: String
  )

  final case class State(
    name: String = "",
    sex: Byte = -1,
    skinColorIndex: Byte = -1,
    hairColorIndex: Byte = -1,
    pupilColorIndex: Byte = -1,
    skinColorFactor: Byte = -1,
    hairColorFactor: Byte = -1,
    clothIndex: Byte = -1,
    faceIndex: Byte = -1,
    breed: Short = -1,
    xp: Long = 0,
    activeEquipmentSheet: Byte = 0,
    title: Short = -1,
    isNew: Boolean = true,
    nationId: Int = 0,
    guildId: Int = -1,
    instanceId: Short = 0
     // add more later...
  )

  sealed trait Cmd

  final case object GetCharacterListData extends Cmd

  sealed trait Evt
}

class Character(id: Long, ownerId: Long) extends SemiPersistentActor {
  import Character._

  def this(data: Character.CharacterCreationData, ownerId: Long) {
    this(data.id, ownerId)
    setState(
      State(
        name = data.name,
        sex = data.sex,
        skinColorIndex = data.skinColorIndex,
        hairColorIndex = data.hairColorIndex,
        pupilColorIndex = data.pupilColorIndex,
        skinColorFactor = data.skinColorFactor,
        hairColorFactor = data.hairColorFactor,
        clothIndex = data.clothIndex,
        faceIndex = data.faceIndex,
        breed = data.breed
      )
    )
  }

  override type State = Character.State
  override type Event = Character.Evt

  override def initialState: State = State()

  override def updateState(st: State, ev: Event): State = ev match {
    case _ => st
  }

  override def elseReceiveCommand: Receive = {
    case GetCharacterListData =>
      import com.github.wakfutcp.protocol.raw.CharacterSerialized._
      import shapeless._

      val st = getState

      sender() !
        ForCharacterListSet(
          Id(id) :: // generate a unique character id
            Identity(0, ownerId) ::
            Name(st.name) ::
            Breed(st.breed) ::
            ActiveEquipmentSheet(st.activeEquipmentSheet) ::
            Appearance(
              st.sex,
              st.skinColorIndex,
              st.hairColorIndex,
              st.pupilColorIndex,
              st.skinColorFactor,
              st.hairColorFactor,
              st.clothIndex,
              st.faceIndex,
              st.title
            ) ::
            EquipmentAppearance(Array()) ::
            CreationData(
              Some(
                CreationDataCreationData(
                  newCharacter = st.isNew,
                  needsRecustom = false,
                  0,
                  needInitialXp = false
                )
              )
            ) ::
            Xp(st.xp) ::
            NationId(st.nationId) ::
            GuildId(st.guildId) ::
            GuildBlazon(0) ::
            InstanceId(st.instanceId) ::
            HNil
        )
  }

  override def persistenceId: String = s"character-$id"
}
