package goldenrose01.enchlib.config

/**
 * Wrapper comodo se vuoi invocare il bootstrap da più punti (es. dal main mod init).
 */
object WorldConfigBootstrap {
    fun register() {
        ConfigBootstrap.registerServerHooks()
    }
}
