package foo.starred.athen.api.storage

import foo.starred.athen.Athen
import foo.starred.kontext.api.storage.impl.generic.impl.GenericJsonStore

class JsonStore(path: String, tts: Int = 15) : GenericJsonStore(Athen.Data.id, path, tts)
