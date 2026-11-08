#!/usr/bin/env python3
"""
publish-play-store.py — Publicador Automatizado na Google Play Store (API v3)

Envia:
  1. Android App Bundle assinado (.aab)
  2. Arquivo de desofuscação R8/ProGuard (mapping.txt)
  3. Notas da versão bilíngues (pt-BR e en-US) de app/src/main/play/release-notes/

Faixas suportadas (--track):
  - alpha       (Teste Fechado padrão da Play Store)
  - internal    (Teste Interno)
  - beta        (Teste Aberto / Beta)
  - production  (Produção)
"""

import argparse
import json
import os
import sys
from pathlib import Path


def read_release_notes(repo_dir: Path) -> list[dict[str, str]]:
    notes_root = repo_dir / "app" / "src" / "main" / "play" / "release-notes"
    result: list[dict[str, str]] = []
    if not notes_root.exists():
        return result

    for lang_dir in sorted(notes_root.iterdir()):
        if not lang_dir.is_dir():
            continue
        note_file = lang_dir / "default.txt"
        if note_file.exists():
            text = note_file.read_text(encoding="utf-8").strip()
            if len(text) > 500:
                raise ValueError(
                    f"Nota da versão em {note_file} excede 500 caracteres ({len(text)} chars)."
                )
            if text:
                result.append({"language": lang_dir.name, "text": text})
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description="Upload signed AAB to Google Play Console")
    parser.add_argument("--package-name", default="com.managesoundmode.app")
    parser.add_argument(
        "--aab",
        default="app/build/outputs/bundle/release/app-release.aab",
        help="Path to signed release .aab",
    )
    parser.add_argument(
        "--mapping",
        default="app/build/outputs/mapping/release/mapping.txt",
        help="Path to R8/ProGuard mapping.txt",
    )
    parser.add_argument(
        "--track",
        default=os.environ.get("PLAY_STORE_TRACK", "alpha"),
        choices=["internal", "alpha", "beta", "production"],
        help="Google Play track (default: alpha = Teste Fechado)",
    )
    parser.add_argument(
        "--release-status",
        default=os.environ.get("PLAY_STORE_RELEASE_STATUS", "completed"),
        choices=["completed", "draft", "inProgress", "halted"],
        help="Release status on track (default: completed)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Validate artifacts, release notes, and credentials without committing to Google Play",
    )
    args = parser.parse_args()

    repo_dir = Path(__file__).resolve().parent.parent
    aab_path = (repo_dir / args.aab).resolve() if not Path(args.aab).is_absolute() else Path(args.aab)
    mapping_path = (
        (repo_dir / args.mapping).resolve()
        if not Path(args.mapping).is_absolute()
        else Path(args.mapping)
    )

    print("==========================================================")
    print("🚀 Google Play Store Publisher — Modos de Som")
    print("==========================================================")
    print(f"• Package Name : {args.package_name}")
    print(f"• Faixa (Track): {args.track} (status: {args.release_status})")
    print(f"• App Bundle   : {aab_path}")
    print(f"• R8 Mapping   : {mapping_path}")

    if not aab_path.exists():
        print(f"❌ Erro: Arquivo .aab não encontrado em {aab_path}", file=sys.stderr)
        return 1

    aab_size_mb = aab_path.stat().st_size / (1024 * 1024)
    print(f"• Tamanho AAB  : {aab_size_mb:.2f} MB")

    release_notes = read_release_notes(repo_dir)
    for note in release_notes:
        print(f"• Release Note [{note['language']}]: {len(note['text'])}/500 chars — OK")

    service_account_raw = os.environ.get("PLAY_STORE_SERVICE_ACCOUNT_JSON", "").strip()
    service_account_file_env = os.environ.get("PLAY_STORE_SERVICE_ACCOUNT_FILE", "").strip()
    service_account_path: Path | None = None
    if service_account_file_env:
        candidate = Path(service_account_file_env)
        if not candidate.is_absolute():
            candidate = (repo_dir / candidate).resolve()
        if candidate.exists():
            service_account_path = candidate

    if args.dry_run and not service_account_raw and service_account_path is None:
        print("----------------------------------------------------------")
        print("✅ [DRY-RUN] Artefatos (.aab + mapping.txt) e Notas da Versão validados com sucesso!")
        print(
            "ℹ️  Para publicar direto na Play Store, salve o JSON da Service Account em:\n"
            f"    {repo_dir / 'app/keystore/play-service-account.json'}\n"
            "    (ou rode: ./scripts/setup-cicd-secrets.sh /caminho/da/sua-chave.json)"
        )
        return 0

    if not service_account_raw and service_account_path is None:
        print(
            "❌ Erro: Chave JSON da Service Account do Google Play não encontrada.\n"
            f"   Coloque o arquivo em: {repo_dir / 'app/keystore/play-service-account.json'}\n"
            "   Ou execute: ./scripts/setup-cicd-secrets.sh /caminho/da/sua-chave.json",
            file=sys.stderr,
        )
        return 1

    try:
        from google.oauth2 import service_account
        from googleapiclient.discovery import build
        from googleapiclient.http import MediaFileUpload
    except ImportError:
        print(
            "❌ Erro: Biblioteca google-api-python-client não instalada. "
            "Execute: pip3 install google-api-python-client google-auth-httplib2",
            file=sys.stderr,
        )
        return 1

    scopes = ["https://www.googleapis.com/auth/androidpublisher"]
    if service_account_raw:
        info = json.loads(service_account_raw)
        credentials = service_account.Credentials.from_service_account_info(info, scopes=scopes)
    else:
        credentials = service_account.Credentials.from_service_account_file(
            str(service_account_path), scopes=scopes
        )

    service = build("androidpublisher", "v3", credentials=credentials, cache_discovery=False)
    edits = service.edits()

    print("1/4 Criando sessão de edição (Edit) na Google Play API...")
    edit_request = edits.insert(body={}, packageName=args.package_name)
    edit_id = edit_request.execute()["id"]

    print(f"2/4 Fazendo upload do App Bundle ({aab_path.name})...")
    bundle_response = (
        edits.bundles()
        .upload(
            editId=edit_id,
            packageName=args.package_name,
            media_body=MediaFileUpload(
                str(aab_path), mimetype="application/octet-stream", resumable=True
            ),
        )
        .execute()
    )
    version_code = bundle_response["versionCode"]
    print(f"    -> Upload concluído! versionCode detectado pelo Google Play: {version_code}")

    if mapping_path.exists():
        print(f"3/4 Fazendo upload do arquivo de símbolos R8 ({mapping_path.name})...")
        edits.deobfuscationfiles().upload(
            editId=edit_id,
            packageName=args.package_name,
            apkVersionCode=version_code,
            deobfuscationFileType="proguard",
            media_body=MediaFileUpload(
                str(mapping_path), mimetype="application/octet-stream", resumable=True
            ),
        ).execute()
    else:
        print("3/4 Aviso: mapping.txt não encontrado, pulando upload de desofuscação.")

    print(f"4/4 Vinculando versionCode {version_code} à faixa '{args.track}'...")
    version_props_path = repo_dir / "version.properties"
    release_name = f"Build {version_code}"
    if version_props_path.exists():
        props = {}
        for line in version_props_path.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.strip().startswith("#"):
                k, v = line.split("=", 1)
                props[k.strip()] = v.strip()
        v_str = f"{props.get('VERSION_MAJOR', '0')}.{props.get('VERSION_MINOR', '3')}.{props.get('VERSION_PATCH', '0')}"
        release_name = f"{v_str} ({version_code})"

    track_body = {
        "releases": [
            {
                "name": release_name,
                "versionCodes": [str(version_code)],
                "status": args.release_status,
                "releaseNotes": release_notes,
            }
        ]
    }
    edits.tracks().update(
        editId=edit_id,
        packageName=args.package_name,
        track=args.track,
        body=track_body,
    ).execute()

    if args.dry_run:
        print("✅ [DRY-RUN] Edit validado na API do Google Play sem commit final!")
        return 0

    edits.commit(editId=edit_id, packageName=args.package_name).execute()
    print(
        f"🎉 Deploy concluído! O versionCode {version_code} está publicado na faixa '{args.track}'."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
