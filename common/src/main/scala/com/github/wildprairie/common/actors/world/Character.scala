package com.github.wildprairie.common.actors.world

import akka.actor.Props
import com.github.wakfutcp.protocol.raw.CharacterDataSet.ForCharacterListSet

/**
  * Created by jacek on 20.05.17.
  */
object Character {
  def props(charId: Long, ownerId: Long): Props =
    Props(classOf[Character], charId, ownerId)

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

class Character(charId: Long, ownerId: Long) extends SemiPersistentActor {
  import Character._
  override type State = Character.State
  override type Event = Character.Evt

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

  override def initialState: State = State()

  override def persistenceId: String = s"character-$charId"

  override def updateState(st: State, ev: Event): State = ev match {
    case _ => st
  }

  override def elseReceiveCommand: Receive = {
    case GetCharacterListData =>
      import Data._
      import shapeless._

      sender() !
        ForCharacterListSet(
          id :: identity :: name :: breed :: activeEquipmentSheet :: appearance ::
            equipmentAppearance :: creationData :: xp :: nation :: guild ::
            guildBlazon :: instance :: HNil
        )
  }

  object Data {
    // helpers for serialization
    import com.github.wakfutcp.protocol.raw.CharacterSerialized._

    def id: Id = Id(charId)
    def identity: Identity = Identity(0, ownerId)
    def name: Name = Name(getState.name)
    def breed: Breed = Breed(getState.breed)
    def activeEquipmentSheet: ActiveEquipmentSheet =
      ActiveEquipmentSheet(getState.activeEquipmentSheet)
    def appearance: Appearance = {
      val st = getState
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
      )
    }
    def equipmentAppearance: EquipmentAppearance =
      EquipmentAppearance(Array()) // TODO: handle this
    def creationData: CreationData =
      CreationData(
        Some(
          CreationDataCreationData(
            newCharacter = getState.isNew,
            needsRecustom = false,
            0,
            needInitialXp = false
          )
        )
      )
    def xp: Xp = Xp(getState.xp)
    def nation: NationId =
      NationId(getState.nationId)
    def guild: GuildId =
      GuildId(getState.guildId)
    def guildBlazon: GuildBlazon =
      GuildBlazon(0)
    def instance: InstanceId =
      InstanceId(getState.instanceId)
  }
}
