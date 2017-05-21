package com.github.wildprairie.common.actors.world

import akka.actor.Props
import com.github.wakfutcp.protocol.raw.CharacterDataSet.ForCharacterListSet

/**
  * Created by jacek on 20.05.17.
  */
object Character {
  import com.github.wakfutcp.protocol.raw.CharacterSerialized._

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
    breed: Short = -1,
    appearance: Appearance = Appearance(
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1,
      -1
    ),
    xp: Long = 0,
    nationId: Short = 0,
    guildId: Long = 0,
    guildBlazon: Long = 0,
    instanceId: Short = 0,
    activeEquipmentSheet: Byte = 0,
    creationData: CreationData = CreationData(None),
    equipmentAppearance: EquipmentAppearance = EquipmentAppearance(Array())
  )

  sealed trait Cmd

  final case object GetCharacterListData extends Cmd

  sealed trait Evt
}

class Character(charId: Long, ownerId: Long) extends SemiPersistentActor {
  import Character._
  override type State = Character.State
  override type Event = Character.Evt

  // this constructor is only called to create a new character
  def this(data: Character.CharacterCreationData, ownerId: Long) {
    this(data.id, ownerId)
    import com.github.wakfutcp.protocol.raw.CharacterSerialized._

    setState(
      State(
        name = data.name,
        breed = data.breed,
        appearance = Appearance(
          skinColorIndex = data.skinColorIndex,
          hairColorIndex = data.hairColorIndex,
          pupilColorIndex = data.pupilColorIndex,
          skinColorFactor = data.skinColorFactor,
          hairColorFactor = data.hairColorFactor,
          clothIndex = data.clothIndex,
          faceIndex = data.faceIndex,
          sex = data.sex,
          currentTitle = -1
        )
      )
    )
    // save a snapshot after character creation
    // so that the character can never be recovered
    // into an uninitialized state
    saveSnapshot(getState)
  }

  override def initialState: State = State()

  override def persistenceId: String = s"character-$charId"

  override def updateState(st: State, ev: Event): State = ev match {
    case _ => st
  }

  override def elseReceiveCommand: Receive = {
    case GetCharacterListData =>
      import data._
      import shapeless._

      sender() !
        ForCharacterListSet(
          id :: identity :: name :: breed :: activeEquipmentSheet :: appearance ::
            equipmentAppearance :: creationData :: xp :: nationId :: guildId ::
            guildBlazon :: instanceId :: HNil
        )
  }

  object data {
    // helpers for serialization
    import com.github.wakfutcp.protocol.raw.CharacterSerialized._

    def id: Id = Id(charId)
    def identity: Identity = Identity(0, ownerId)
    def name: Name = Name(getState.name)
    def breed: Breed = Breed(getState.breed)
    def activeEquipmentSheet: ActiveEquipmentSheet =
      ActiveEquipmentSheet(getState.activeEquipmentSheet)
    def appearance: Appearance = getState.appearance
    def equipmentAppearance: EquipmentAppearance = getState.equipmentAppearance
    def creationData: CreationData = getState.creationData
    def xp: Xp = Xp(getState.xp)
    def nationId: NationId = NationId(getState.nationId)
    def guildId: GuildId = GuildId(getState.guildId)
    def guildBlazon: GuildBlazon = GuildBlazon(0)
    def instanceId: InstanceId = InstanceId(getState.instanceId)
  }
}
