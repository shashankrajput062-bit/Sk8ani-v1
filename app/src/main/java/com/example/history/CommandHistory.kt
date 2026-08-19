package com.example.history

import com.example.scene.MaterialData
import com.example.scene.Scene
import com.example.scene.SceneObject
import com.example.scene.Transform

interface SceneCommand {
    val description: String
    fun execute(scene: Scene)
    fun undo(scene: Scene)
}

class TransformCommand(
    private val objectId: String,
    private val oldTransform: Transform,
    private val newTransform: Transform
) : SceneCommand {
    override val description: String = "Transform Object"

    override fun execute(scene: Scene) {
        scene.findObjectById(objectId)?.transform = newTransform.copy()
    }

    override fun undo(scene: Scene) {
        scene.findObjectById(objectId)?.transform = oldTransform.copy()
    }
}

class AddObjectCommand(
    private val sceneObject: SceneObject
) : SceneCommand {
    override val description: String = "Add ${sceneObject.name}"

    override fun execute(scene: Scene) {
        if (scene.findObjectById(sceneObject.id) == null) {
            scene.addObject(sceneObject)
        }
    }

    override fun undo(scene: Scene) {
        scene.removeObject(sceneObject.id)
    }
}

class DeleteObjectCommand(
    private val sceneObject: SceneObject
) : SceneCommand {
    override val description: String = "Delete ${sceneObject.name}"

    override fun execute(scene: Scene) {
        scene.removeObject(sceneObject.id)
    }

    override fun undo(scene: Scene) {
        if (scene.findObjectById(sceneObject.id) == null) {
            scene.addObject(sceneObject)
        }
    }
}

class MaterialCommand(
    private val objectId: String,
    private val oldMaterial: MaterialData,
    private val newMaterial: MaterialData
) : SceneCommand {
    override val description: String = "Change Material"

    override fun execute(scene: Scene) {
        scene.findObjectById(objectId)?.material = newMaterial.copy()
    }

    override fun undo(scene: Scene) {
        scene.findObjectById(objectId)?.material = oldMaterial.copy()
    }
}

class CommandHistory(private val maxHistory: Int = 50) {
    private val undoStack = mutableListOf<SceneCommand>()
    private val redoStack = mutableListOf<SceneCommand>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun executeCommand(command: SceneCommand, scene: Scene) {
        command.execute(scene)
        undoStack.add(command)
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun undo(scene: Scene): SceneCommand? {
        if (!canUndo) return null
        val cmd = undoStack.removeAt(undoStack.lastIndex)
        cmd.undo(scene)
        redoStack.add(cmd)
        return cmd
    }

    fun redo(scene: Scene): SceneCommand? {
        if (!canRedo) return null
        val cmd = redoStack.removeAt(redoStack.lastIndex)
        cmd.execute(scene)
        undoStack.add(cmd)
        return cmd
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
